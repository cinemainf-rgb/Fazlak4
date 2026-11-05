package com.fazlaka.netmanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.*
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var client: MikroTikClient518
    private var profiles: List<MikroTikClient518.Profile> = emptyList()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24,24,24,24)
        }
        
        val hostInput = EditText(this).apply { hint = "IP 10.0.0.1"; setText("10.0.0.1") }
        val userInput = EditText(this).apply { hint = "User admin"; setText("admin") }
        val passInput = EditText(this).apply { hint = "Password" }
        val connectBtn = Button(this).apply { text = "اتصال مباشر 5.18 بدون وسيط" }
        val profileSpinner = Spinner(this)
        val quotaInput = EditText(this).apply { hint = "الجيجات (انت تختار) 20"; setText("20") }
        val priceInput = EditText(this).apply { hint = "السعر (انت تختار) 35"; setText("35") }
        val digitsSpinner = Spinner(this)
        val countInput = EditText(this).apply { hint = "عدد الكروت"; setText("10") }
        val generateBtn = Button(this).apply { text = "توليد وطباعة - أرقام بس بدون بادئة" }
        val resultText = TextView(this).apply { text = "البرنامج شغال بكل الأوبشنات\n- يقرا البروفايل 20GB يحط 20GB في التوتال\n- أرقام بس 7-8 أرقام\n- بدون باسورد\n- انت تختار السعر والجيجات واللي تختاره يطبع" }
        
        layout.addView(hostInput)
        layout.addView(userInput)
        layout.addView(passInput)
        layout.addView(connectBtn)
        layout.addView(profileSpinner)
        layout.addView(quotaInput)
        layout.addView(priceInput)
        layout.addView(digitsSpinner)
        layout.addView(countInput)
        layout.addView(generateBtn)
        layout.addView(resultText)
        
        setContentView(layout)
        
        val digitsOptions = arrayOf("7 أرقام", "8 أرقام")
        digitsSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, digitsOptions)
        
        connectBtn.setOnClickListener {
            client = MikroTikClient518(hostInput.text.toString(), userInput.text.toString(), passInput.text.toString())
            CoroutineScope(Dispatchers.IO).launch {
                val ok = client.connect()
                if (ok) {
                    profiles = client.readProfiles()
                    withContext(Dispatchers.Main) {
                        val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, profiles.map { "${it.name} - ${it.quotaGb}GB - ${it.price}ج" })
                        profileSpinner.adapter = adapter
                        resultText.text = "تم الاتصال - تم قراءة ${profiles.size} بروفايل من 5.18"
                    }
                } else {
                    withContext(Dispatchers.Main) { resultText.text = "فشل الاتصال - تأكد من IP والباسورد ونفس شبكة الميكروتيك" }
                }
            }
        }
        
        generateBtn.setOnClickListener {
            val pos = profileSpinner.selectedItemPosition
            if (pos < 0 || profiles.isEmpty()) {
                resultText.text = "اختار بروفايل أولا"
                return@setOnClickListener
            }
            val profile = profiles[pos]
            val customQuota = quotaInput.text.toString().toIntOrNull() ?: profile.quotaGb
            val customPrice = priceInput.text.toString().toIntOrNull() ?: profile.price
            val count = countInput.text.toString().toIntOrNull() ?: 10
            val digits = if (digitsSpinner.selectedItemPosition==0) 7 else 8
            
            CoroutineScope(Dispatchers.IO).launch {
                val cards = client.generateCards(profile.name, customQuota, customPrice, count, digits)
                val totalQuota = customQuota * count
                val totalPrice = customPrice * count
                withContext(Dispatchers.Main) {
                    resultText.text = "تم توليد ${count} كارت\nرقم مثال: ${cards.firstOrNull()?.number}\nكل كارت: ${customQuota}GB\nالتوتال: ${totalQuota}GB\nالمبيعات: ${totalPrice}ج\nشكل الطباعة:\nFAZLAKA\n${cards.firstOrNull()?.number}\n${customPrice}ج      ${customQuota}GB"
                }
            }
        }
    }
}
