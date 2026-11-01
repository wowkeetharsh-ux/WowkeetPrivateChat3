const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const SOUND_CHANNEL_ID = "wowkeet_private_chat_v2";
const SOUND_NAME = "wowkeet_order_alert";

// Listen to new messages under chats/{sellerId}/messages/{messageId}
exports.onChatMessageCreated = functions.database
  .ref("/chats/{sellerId}/messages/{messageId}")
  .onCreate(async (snapshot, context) => {
    const { sellerId, messageId } = context.params;
    const msg = snapshot.val();

    if (!msg || msg.deleted) return null;

    const senderRole = msg.senderRole; // "admin" or "seller"
    const senderName = msg.senderName || (senderRole === "admin" ? "Admin" : "Seller");
    const messageText = msg.text || "New message";

    let tokenRefs = [];

    if (senderRole === "admin") {
      // Send FCM ONLY to this seller
      const sellerTokensSnap = await admin.database().ref(`/pushTokens/sellers/${sellerId}`).once("value");
      if (sellerTokensSnap.exists()) {
        sellerTokensSnap.forEach((child) => {
          if (child.val()) tokenRefs.push(child.val());
        });
      }
    } else if (senderRole === "seller") {
      // Send FCM ONLY to Admins
      const adminTokensSnap = await admin.database().ref("/pushTokens/admin").once("value");
      if (adminTokensSnap.exists()) {
        adminTokensSnap.forEach((child) => {
          if (child.val()) tokenRefs.push(child.val());
        });
      }
    }

    if (tokenRefs.length === 0) {
      console.log(`No registered push tokens found for recipient of message ${messageId}`);
      return null;
    }

    const payload = {
      data: {
        type: "chat_message",
        sellerId: sellerId,
        messageId: messageId,
        senderRole: senderRole,
        title: senderName,
        body: messageText
      },
      android: {
        priority: "high",
        notification: {
          channelId: SOUND_CHANNEL_ID,
          sound: SOUND_NAME,
          defaultSound: false,
          priority: "high",
          visibility: "public"
        }
      }
    };

    const response = await admin.messaging().sendEachForMulticast({
      tokens: tokenRefs,
      ...payload
    });

    return null;
  });

// Listen to Seller Shop Status Toggle (Open/Close)
exports.onShopStatusChanged = functions.database
  .ref("/sellers/{sellerId}/shopOpen")
  .onUpdate(async (change, context) => {
    const { sellerId } = context.params;
    const isNowOpen = change.after.val();
    const updatedByRole = (await admin.database().ref(`/sellers/${sellerId}/lastStatusUpdatedBy`).once("value")).val() || "seller";

    const sellerSnap = await admin.database().ref(`/sellers/${sellerId}`).once("value");
    const sellerData = sellerSnap.val() || {};
    const sellerName = sellerData.name || `Seller ${sellerId}`;

    let tokens = [];
    let title = isNowOpen ? "Shop Open" : "Shop Closed";
    let body = "";

    if (updatedByRole === "seller") {
      body = `${sellerName} ${isNowOpen ? "opened" : "closed"} the shop.`;
      const adminTokensSnap = await admin.database().ref("/pushTokens/admin").once("value");
      if (adminTokensSnap.exists()) {
        adminTokensSnap.forEach((child) => {
          if (child.val()) tokens.push(child.val());
        });
      }
    } else {
      body = `Admin ${isNowOpen ? "opened" : "closed"} your shop.`;
      const sellerTokensSnap = await admin.database().ref(`/pushTokens/sellers/${sellerId}`).once("value");
      if (sellerTokensSnap.exists()) {
        sellerTokensSnap.forEach((child) => {
          if (child.val()) tokens.push(child.val());
        });
      }
    }

    if (tokens.length === 0) return null;

    return admin.messaging().sendEachForMulticast({
      tokens: tokens,
      data: {
        type: "shop_status",
        sellerId: sellerId,
        title: title,
        body: body
      },
      android: {
        priority: "high",
        notification: {
          channelId: SOUND_CHANNEL_ID,
          sound: SOUND_NAME,
          defaultSound: false,
          priority: "high",
          visibility: "public"
        }
      }
    });
  });