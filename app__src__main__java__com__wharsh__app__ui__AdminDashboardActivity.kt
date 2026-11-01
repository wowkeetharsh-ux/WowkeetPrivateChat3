package com.wharsh.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.wharsh.app.databinding.ActivityAdminDashboardBinding
import com.wharsh.app.models.ChatMessage
import com.wharsh.app.models.SellerInfo
import com.wharsh.app.ui.adapters.SellerAdapter

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private val allSellers = mutableListOf<SellerInfo>()
    private lateinit var adapter: SellerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = SellerAdapter(
            onSellerClick = { seller ->
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("sellerId", seller.id)
                    putExtra("sellerName", seller.name)
                    putExtra("userRole", "admin")
                }
                startActivity(intent)
            },
            onToggleShop = { seller ->
                toggleSellerShopStatus(seller)
            }
        )

        binding.rvSellers.layoutManager = LinearLayoutManager(this)
        binding.rvSellers.adapter = adapter

        setupSearch()
        loadSellers()
    }

    private fun setupSearch() {
        binding.etSearchSeller.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String) {
        val q = query.lowercase().trim()
        val filtered = allSellers.filter {
            it.name.lowercase().contains(q) || it.id.lowercase().contains(q) || it.mobile.contains(q)
        }
        adapter.submitList(filtered)
    }

    private fun loadSellers() {
        val db = FirebaseDatabase.getInstance().reference

        db.child("sellers").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allSellers.clear()
                for (child in snapshot.children) {
                    val id = child.key ?: continue
                    val name = child.child("name").getValue(String::class.java) ?: "Seller $id"
                    val mobile = child.child("mobile").getValue(String::class.java) ?: ""
                    val shopOpen = child.child("shopOpen").getValue(Boolean::class.java) ?: false

                    val seller = SellerInfo(id = id, name = name, mobile = mobile, shopOpen = shopOpen)
                    attachLastMessageListener(seller)
                    allSellers.add(seller)
                }
                adapter.submitList(ArrayList(allSellers))
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AdminDashboardActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun attachLastMessageListener(seller: SellerInfo) {
        val msgQuery = FirebaseDatabase.getInstance().reference
            .child("chats").child(seller.id).child("messages")
            .orderByChild("timestamp")
            .limitToLast(20)

        msgQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unread = 0
                var lastMsgText = ""
                var lastMsgTime = 0L

                for (c in snapshot.children) {
                    val msg = c.getValue(ChatMessage::class.java) ?: continue
                    if (msg.senderRole == "seller" && !msg.seen) {
                        unread++
                    }
                    lastMsgText = msg.text
                    lastMsgTime = msg.getTimestampLong()
                }

                seller.unreadCount = unread
                seller.lastMessage = lastMsgText
                seller.lastMessageTime = lastMsgTime
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun toggleSellerShopStatus(seller: SellerInfo) {
        val targetState = !seller.shopOpen
        val db = FirebaseDatabase.getInstance().reference.child("sellers").child(seller.id)
        val updates = mapOf<String, Any>(
            "shopOpen" to targetState,
            "lastStatusUpdatedBy" to "admin"
        )
        db.updateChildren(updates)
    }
}