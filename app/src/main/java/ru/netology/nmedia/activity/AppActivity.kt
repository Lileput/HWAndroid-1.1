package ru.netology.nmedia.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.ActivityAppBinding
import ru.netology.nmedia.util.MapKitInit
import ru.netology.nmedia.util.StringArg
import javax.inject.Inject

@AndroidEntryPoint
class AppActivity : AppCompatActivity() {

    @Inject
    lateinit var appAuth: AppAuth

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var authMenuVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        MapKitInit.ensureInitialized(this)
        super.onCreate(savedInstanceState)
        val binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.feetFragment, R.id.eventsFragment, R.id.usersFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.bottomNavigation.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { controller, destination, _ ->
            authMenuVisible = destination.id !in AUTH_MENU_HIDDEN_DESTINATIONS
            invalidateMenu()

            when (destination.id) {
                R.id.singlePostFragment,
                R.id.singleEventFragment,
                -> supportActionBar?.setDisplayHomeAsUpEnabled(true)
                in DETAIL_DESTINATIONS ->
                    supportActionBar?.setDisplayHomeAsUpEnabled(controller.previousBackStackEntry != null)
            }

            when (destination.id) {
                R.id.signInFragment,
                R.id.signUpFragment,
                R.id.newPostFragment,
                R.id.singlePostFragment,
                R.id.profileFragment,
                R.id.chooseUsersFragment,
                R.id.postMapFragment,
                R.id.postLikersFragment,
                R.id.newEventFragment,
                R.id.singleEventFragment,
                R.id.newJobFragment,
                -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> binding.bottomNavigation.visibility = View.VISIBLE
            }
        }

        requestNotificationsPermission()

        intent?.let {
            if (it.action != Intent.ACTION_SEND) return@let
            val text = it.getStringExtra(Intent.EXTRA_TEXT)
            if (text.isNullOrBlank()) {
                Snackbar.make(binding.root, R.string.error_empty_content, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) { finish() }
                    .show()
                return@let
            }

            if (appAuth.authState.value == null) {
                navController.navigate(R.id.signInFragment)
            } else {
                navController.navigate(
                    R.id.newPostFragment,
                    Bundle().apply { textArg = text },
                )
            }
        }

        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_auth, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                menu.findItem(R.id.auth)?.isVisible = authMenuVisible
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    R.id.auth -> {
                        val userId = appAuth.authState.value?.resolvedId()
                        if (userId != null && userId != 0L) {
                            navController.navigate(
                                R.id.profileFragment,
                                Bundle().apply { putLong("userId", userId) },
                            )
                        } else {
                            navController.navigate(R.id.signInFragment)
                        }
                        true
                    }
                    else -> false
                }
        }, this, Lifecycle.State.RESUMED)
    }

    companion object {
        var Bundle.textArg: String? by StringArg

        private val DETAIL_DESTINATIONS = setOf(
            R.id.newPostFragment,
            R.id.newEventFragment,
            R.id.newJobFragment,
            R.id.chooseUsersFragment,
            R.id.postMapFragment,
            R.id.postLikersFragment,
            R.id.photoViewFragment,
        )

        private val AUTH_MENU_HIDDEN_DESTINATIONS = setOf(
            R.id.signInFragment,
            R.id.signUpFragment,
            R.id.newPostFragment,
            R.id.singlePostFragment,
            R.id.chooseUsersFragment,
            R.id.postMapFragment,
            R.id.postLikersFragment,
            R.id.newEventFragment,
            R.id.singleEventFragment,
            R.id.profileFragment,
            R.id.newJobFragment,
        )
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(permission), 1)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}