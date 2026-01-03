package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.SubMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(view ->
                Snackbar.make(view, "Explore the app screens from the drawer.", Snackbar.LENGTH_LONG)
                        .setAnchorView(R.id.fab)
                        .show()
        );
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        configureNavigationMenu(navigationView);

        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_products) {
                startActivity(new Intent(this, ProductActivity.class));
            } else if (id == R.id.nav_fragments) {
                startActivity(new Intent(this, FragmentsActivity.class));
            } else if (id == R.id.nav_data_send) {
                startActivity(new Intent(this, DataSendActivity.class));
            } else if (id == R.id.nav_user_info) {
                startActivity(new Intent(this, UserInfoActivity.class));
            } else if (id == R.id.nav_drawer_demo) {
                startActivity(new Intent(this, DrawerActivity.class));
            } else if (id == R.id.nav_login) {
                startActivity(new Intent(this, LoginActivity.class));
            } else if (id == R.id.nav_signup) {
                startActivity(new Intent(this, SignupActivity.class));
            } else if (id == R.id.nav_sign_out) {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut();
                Intent signOutIntent = new Intent(this, LoginActivity.class);
                signOutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(signOutIntent);
                finishAffinity();
                drawer.closeDrawer(GravityCompat.START);
                return true;
            } else {
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                if (handled) {
                    drawer.closeDrawer(GravityCompat.START);
                }
                return handled;
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void configureNavigationMenu(NavigationView navigationView) {
        Menu menu = navigationView.getMenu();
        menu.clear();

        menu.add(Menu.NONE, R.id.nav_home, Menu.NONE, getString(R.string.menu_home))
                .setIcon(R.drawable.ic_menu_camera);
        menu.add(Menu.NONE, R.id.nav_gallery, Menu.NONE, getString(R.string.menu_gallery))
                .setIcon(R.drawable.ic_menu_gallery);
        menu.add(Menu.NONE, R.id.nav_slideshow, Menu.NONE, getString(R.string.menu_slideshow))
                .setIcon(R.drawable.ic_menu_slideshow);

        SubMenu subMenu = menu.addSubMenu(getString(R.string.menu_section_screens));
        subMenu.add(Menu.NONE, R.id.nav_products, Menu.NONE, getString(R.string.menu_products))
                .setIcon(R.drawable.ic_menu_gallery);
        subMenu.add(Menu.NONE, R.id.nav_fragments, Menu.NONE, getString(R.string.menu_fragments))
                .setIcon(R.drawable.ic_menu_slideshow);
        subMenu.add(Menu.NONE, R.id.nav_data_send, Menu.NONE, getString(R.string.menu_data_send))
                .setIcon(R.drawable.ic_menu_camera);
        subMenu.add(Menu.NONE, R.id.nav_user_info, Menu.NONE, getString(R.string.menu_user_info))
                .setIcon(R.drawable.ic_menu_camera);
        subMenu.add(Menu.NONE, R.id.nav_drawer_demo, Menu.NONE, getString(R.string.menu_drawer_demo))
                .setIcon(R.drawable.ic_menu_slideshow);
        subMenu.add(Menu.NONE, R.id.nav_login, Menu.NONE, getString(R.string.menu_login))
                .setIcon(R.drawable.ic_menu_camera);
        subMenu.add(Menu.NONE, R.id.nav_signup, Menu.NONE, getString(R.string.menu_signup))
                .setIcon(R.drawable.ic_menu_gallery);
        subMenu.add(Menu.NONE, R.id.nav_sign_out, Menu.NONE, getString(R.string.menu_sign_out))
                .setIcon(R.drawable.ic_menu_camera);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Session handling - Check if user is authenticated
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // User is not authenticated, redirect to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        }
    }
}

