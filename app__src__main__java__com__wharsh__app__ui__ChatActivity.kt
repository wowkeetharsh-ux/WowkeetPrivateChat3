package com.wharsh.app.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.wharsh.app.R
import com.wharsh.app.WowkeetApp
import com.wharsh.app.databinding.ActivityChatBinding
import com.wharsh.app.models.ChatMessage
import com.wharsh.app.ui.adapters.ChatAdapter

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var adapter: ChatAdapter
    private val messagesList = mutableListOf<ChatMessage>()

    private var sellerId: String = ""
    private var role: String = "seller"
    private var currentUserId: String = ""

    private lateinit var messagesRef: DatabaseReference
    private var messagesListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        sellerId = intent.getStringExtra("sellerId") ?: currentUserId
        val sellerName = intent.getStringExtra("sellerName") ?: "Support Chat"
        role = intent.getStringExtra("userRole") ?: "seller"

        setSupportActionBar(binding.toolbarChat)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = sellerName

        binding.toolbarChat.setNavigationOnClickListener { finish() }

        WowkeetApp.activeChatSellerId = sellerId

        messagesRef = FirebaseDatabase.getInstance().reference.child("chats").child(sellerId).child("messages")

        setupRecyclerView()
        setupSendButton()
        listenForMessages()
    }

    override fun onResume() {
        super.onResume()
        WowkeetApp.activeChatSellerId = sellerId
        markIncomingAsRead()
    }

    override fun onPause() {
        super.onPause()
        WowkeetApp.activeChatSellerId = null
    }

    override fun onDestroy() {
        super.onDestroy()
        messagesListener?.let { messagesRef.removeEventListener(it) }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(currentUserId) { msg ->
            if (msg.senderId == currentUserId && !msg.deleted) {
                showDeleteMessageDialog(msg)
            }
        }
        val layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.rvMessages.layoutManager = layoutManager
        binding.rvMessages.adapter = adapter
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessageInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            val messageId = messagesRef.push().key ?: return@setOnClickListener
            val user = FirebaseAuth.getInstance().currentUser
            val senderName = if (role == "admin") "Admin" else (user?.email?.substringBefore("@") ?: "Seller")

            val messageMap = hashMapOf(
                "id" to messageId,
                "senderId" to currentUserId,
                "senderRole" to role,
                "senderName" to senderName,
                "text" to text,
                "time" to ServerValue.TIMESTAMP,
                "timestamp" to ServerValue.TIMESTAMP,
                "status" to "sent",
                "seen" to false,
                "deleted" to false
            )

            binding.etMessageInput.setText("")
            messagesRef.child(messageId).setValue(messageMap)
        }
    }

    private fun listenForMessages() {
        messagesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                messagesList.clear()
                for (child in snapshot.children) {
                    val msg = child.getValue(ChatMessage::class.java) ?: continue
                    messagesList.add(msg)
                }
                adapter.submitList(ArrayList(messagesList))
                if (messagesList.isNotEmpty()) {
                    binding.rvMessages.scrollToPosition(messagesList.size - 1)
                }
                markIncomingAsRead()
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        messagesRef.orderByChild("timestamp").addValueEventListener(messagesListener!!)
    }

    private fun markIncomingAsRead() {
        for (msg in messagesList) {
            if (msg.senderId != currentUserId && !msg.seen) {
                val updates = mapOf<String, Any>(
                    "seen" to true,
                    "status" to "read"
                )
                messagesRef.child(msg.id).updateChildren(updates)
            }
        }
    }

    private fun showDeleteMessageDialog(message: ChatMessage) {
        AlertDialog.Builder(this)
            .setTitle("Delete Message")
            .setMessage("Delete for everyone?")
            .setPositiveButton("Delete") { _, _ ->
                val updates = mapOf<String, Any>(
                    "text" to getString(R.string.msg_deleted),
                    "deleted" to true
                )
                messagesRef.child(message.id).updateChildren(updates)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu?.add(0, 1, 0, "Clear chat")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to clear all messages in this conversation?")
                .setPositiveButton("Clear") { _, _ ->
                    messagesRef.removeValue().addOnSuccessListener {
                        Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}