package org.hcilab.projects.nlogx.ui;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.navigation.NavigationView;

import org.hcilab.projects.nlogx.R;
import org.hcilab.projects.nlogx.misc.Const;
import org.hcilab.projects.nlogx.misc.DatabaseHelper;
import org.hcilab.projects.nlogx.misc.ExportTask;
import org.hcilab.projects.nlogx.misc.Util;
import org.hcilab.projects.nlogx.service.NotificationHandler;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

	private DrawerLayout drawer;
	private NavigationView navigationView;
	private Button buttonAllow;
	private FrameLayout frameLayout;
	private AlertDialog dialog = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setTitle("");

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		drawer = findViewById(R.id.drawer_layout);
		navigationView = findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
				R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		frameLayout = findViewById(R.id.fragment_container);

		buttonAllow = findViewById(R.id.button_allow);
		buttonAllow.setOnClickListener(view -> {
			startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
		});

		if (savedInstanceState == null && Util.isNotificationAccessEnabled(getApplicationContext())) {
			Bundle bundle = new Bundle();
			bundle.putString("selected_navigation", "Recents");
			RecentsFragment recentsFragment = new RecentsFragment();
			recentsFragment.setArguments(bundle);
			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
					recentsFragment).commit();
			navigationView.setCheckedItem(R.id.nav_recents);
		}else {
			openDialog();
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!Util.isNotificationAccessEnabled(getApplicationContext())) {
			openDialog();
		}else {
			buttonAllow.setVisibility(View.GONE);
			frameLayout.setVisibility(View.VISIBLE);
			Bundle bundle = new Bundle();
			bundle.putString("selected_navigation", "Recents");
			RecentsFragment recentsFragment = new RecentsFragment();
			recentsFragment.setArguments(bundle);
			getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
					recentsFragment).commit();
			navigationView.setCheckedItem(R.id.nav_recents);
		}

	}

	private void openDialog() {

		setTitle("");

		frameLayout.setVisibility(View.GONE);
		buttonAllow.setVisibility(View.VISIBLE);



		AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setTitle("Alert!")
				.setMessage("Please allow permission to use app.")
				.setCancelable(true)
				.setPositiveButton("OK", null);

		if(dialog != null){
			dialog.dismiss();
		}

		dialog = builder.show();

	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_delete:
				if (Util.isNotificationAccessEnabled(getApplicationContext())){
					confirm();
				}else {
					openDialog();
				}
				return true;
			case R.id.menu_export:
				if (Util.isNotificationAccessEnabled(getApplicationContext())){
					export();
				}else {
					openDialog();
				}
				return true;

		}
		return super.onOptionsItemSelected(item);
	}

	private void confirm() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogStyle);
		builder.setTitle(R.string.dialog_delete_header);
		builder.setMessage(R.string.dialog_delete_text);
		builder.setNegativeButton(R.string.dialog_delete_no, (dialogInterface, i) -> {});
		builder.setPositiveButton(R.string.dialog_delete_yes, (dialogInterface, i) -> truncate());
		builder.show();
	}

	private void truncate() {
		try {
			DatabaseHelper dbHelper = new DatabaseHelper(this);
			SQLiteDatabase db = dbHelper.getWritableDatabase();
			db.execSQL(DatabaseHelper.SQL_DELETE_ENTRIES_POSTED);
			db.execSQL(DatabaseHelper.SQL_CREATE_ENTRIES_POSTED);
			db.execSQL(DatabaseHelper.SQL_DELETE_ENTRIES_REMOVED);
			db.execSQL(DatabaseHelper.SQL_CREATE_ENTRIES_REMOVED);
			Intent local = new Intent();
			local.setAction(NotificationHandler.BROADCAST);
			LocalBroadcastManager.getInstance(this).sendBroadcast(local);
		} catch (Exception e) {
			if(Const.DEBUG) e.printStackTrace();
		}
	}

	private void export() {
		if(!ExportTask.exporting) {
			ExportTask exportTask = new ExportTask(this, findViewById(android.R.id.content));
			exportTask.execute();
		}
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		switch (item.getItemId()) {

			case R.id.nav_recents:
				if (Util.isNotificationAccessEnabled(getApplicationContext())){
					Bundle bundle = new Bundle();
					bundle.putString("selected_navigation", "Recents");
					RecentsFragment recentsFragment = new RecentsFragment();
					recentsFragment.setArguments(bundle);
					getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
							recentsFragment).commit();
				}else {
					openDialog();
				}
				break;

			case R.id.nav_all_notifications:
			case R.id.nav_notifications_by_apps:
			case R.id.nav_how_to_use:
			case R.id.nav_favourites:
				if (Util.isNotificationAccessEnabled(getApplicationContext())){
					Toast.makeText(MainActivity.this, "Not enabled yet!", Toast.LENGTH_SHORT).show();
				}else {
					openDialog();
				}
				break;

			case R.id.nav_export_logs:
				if (Util.isNotificationAccessEnabled(getApplicationContext())){
					export();
				}else {
					openDialog();
				}
				break;

			case R.id.nav_settings:
				if (Util.isNotificationAccessEnabled(getApplicationContext())){
					getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container,
							new SettingsFragment()).commit();
				}else {
					openDialog();
				}
				break;
		}

		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	@Override
	public void onBackPressed() {
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}
}