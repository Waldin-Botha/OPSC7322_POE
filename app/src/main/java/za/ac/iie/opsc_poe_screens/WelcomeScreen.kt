package za.ac.iie.opsc_poe_screens

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity

import za.ac.iie.opsc_poe_screens.databinding.ActivityWelcomeScreenBinding

class WelcomeScreen : AppCompatActivity() {
    private lateinit var _btnSignUp: Button
    private lateinit var _btnSignIn: Button
    private lateinit var _tvAppName: TextView
    private lateinit var _lHolder: LinearLayout
    private lateinit var _tvH2Text: TextView
    private lateinit var _tvH3Text: TextView
    private lateinit var _tvNoAccount: TextView
    private lateinit var _imgLogo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.hide()

        // Hide top and bottom UI
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_screen)

        // View binding setup
        val binding = ActivityWelcomeScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            _btnSignIn = btnSignIn
            _btnSignUp = btnSignUp
            _tvNoAccount = tvNoAccount
            _tvH2Text = tvH2Text
            _tvH3Text = tvH3Text
            _tvAppName = tvAppName
            _lHolder = lHolder
            _imgLogo = imgLogo
        }

        //Navigate to the Sign In Screen
        _btnSignIn.setOnClickListener {
            val intent = Intent(this, SignIn::class.java)
            startActivity(intent)
        }

        //Navigate to the Sign Up Screen
        _btnSignUp.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

    }//OnCreate
}