package de.martinwolff.hotdog_nothotdog_kotlin_new

// Your IDE likely can auto-import these classes, but there are several
// different implementations so we list them here to disambiguate
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import de.martinwolff.hotdog_nothotdog_kotlin_new.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    // Falls Probleme: Android dev auf udacity Lesson 2, Abschnitt 15
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    }
}
