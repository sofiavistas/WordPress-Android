package org.wordpress.android.ui.reader;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.ui.reader.views.ReaderFollowButton;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.PermissionUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPPermissionUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ReaderContactsActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private RecyclerView mRecycler;
    private UserAdapter mAdapter;
    private int mAvatarSz;
    private final HashSet<Long> mFollowedBlogIds = new HashSet<>();
    private static final String[] mPermissions = { Manifest.permission.READ_CONTACTS };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.reader_activity_contacts);
        mRecycler = findViewById(R.id.recycler);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(getAdapter());

        mAvatarSz = getResources().getDimensionPixelSize(R.dimen.avatar_sz_small);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mFollowedBlogIds.addAll(ReaderBlogTable.getFollowedBlogIds());

        if (PermissionUtils.checkPermissions(this, mPermissions)) {
            loadUsers();
        } else {
            showSoftAskView(true);
        }
    }

    private UserAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new UserAdapter();
        }
        return mAdapter;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSoftAskView(boolean show) {
        View softAskContainer = findViewById(R.id.container_soft_ask);
        if (!show) {
            if (softAskContainer.getVisibility() == View.VISIBLE) {
                AniUtils.fadeOut(softAskContainer, AniUtils.Duration.MEDIUM);
            }
            return;
        }

        final boolean isAlwaysDenied = WPPermissionUtils.isPermissionAlwaysDenied(
                this, Manifest.permission.READ_CONTACTS);

        TextView txtLabel = findViewById(R.id.text_soft_ask_label);
        String appName = "<strong>" + getString(R.string.app_name) + "</strong>";
        String label;
        if (isAlwaysDenied) {
            String permissionName = "<strong>"
                    + WPPermissionUtils.getPermissionName(this, Manifest.permission.READ_CONTACTS)
                    + "</strong>";
            label = String.format(
                    getString(R.string.reader_label_contacts_soft_ask_denied), appName, permissionName);
        } else {
            label = String.format(getString(R.string.reader_label_contacts_soft_ask), appName);
        }
        txtLabel.setText(Html.fromHtml(label));

        TextView txtAllow = softAskContainer.findViewById(R.id.text_soft_ask_allow);
        int allowId = isAlwaysDenied ?
                R.string.button_edit_permissions : R.string.photo_picker_soft_ask_allow;
        txtAllow.setText(allowId);
        txtAllow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isAlwaysDenied) {
                    WPPermissionUtils.showAppSettings(ReaderContactsActivity.this);
                } else {
                    requestPermission();
                }
            }
        });

        softAskContainer.setVisibility(View.VISIBLE);
    }

    private void showEmptyView(boolean show) {
        findViewById(R.id.text_empty).setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void requestPermission() {
        requestPermissions(mPermissions, WPPermissionUtils.CONTACTS_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        boolean allGranted = WPPermissionUtils.setPermissionListAsked(
                this, requestCode, permissions, grantResults, true);
        if (allGranted && requestCode == WPPermissionUtils.CONTACTS_PERMISSION_REQUEST_CODE) {
            showSoftAskView(false);
            loadUsers();
        }
    }

    private void loadUsers() {
        List<String> emailList = getContactEmails();
        // TODO: this is dummy date, need to send email list to backend to get actual users
        ReaderUserList userList = new ReaderUserList();
        long id = 0;
        for (String email: emailList) {
            ReaderUser user = new ReaderUser();
            user.setDisplayName(email);
            user.userId = id;
            user.blogId = 52451191;
            user.setUrl("https://www.nickbradbury.com/");
            user.setAvatarUrl(GravatarUtils.gravatarFromEmail(email, mAvatarSz));
            id++;
            userList.add(user);
        }
        getAdapter().setUsers(userList);
        showEmptyView(userList.isEmpty());
    }

    /*
     * returns a sorted list of unique email addresses from the device's contacts
     */
    private List<String> getContactEmails() {
        HashSet<String> hashList = new HashSet<>();
        List<String> emailList = new ArrayList<>();
        try {
            ContentResolver cr = getContentResolver();
            String[] projection = new String[] { ContactsContract.CommonDataKinds.Email.DATA };
            String sortOrder = ContactsContract.CommonDataKinds.Email.DATA + " COLLATE LOCALIZED ASC";
            String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''";
            Cursor cur = cr.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection, filter, null, sortOrder);
            if (cur != null) {
                while (cur.moveToNext()) {
                    String email = cur.getString(0);
                    if (hashList.add(email)) {
                        emailList.add(email);
                    }
                }
                cur.close();
            }
        } catch (SecurityException e) {
            // no permission
            AppLog.e(AppLog.T.UTILS, e);
        }
        return emailList;
    }

    private boolean isFollowedBlogId(Long blogId) {
        return mFollowedBlogIds.contains(blogId);
    }

    private void toggleFollow(final ReaderFollowButton followButton, final long blogId) {
        if (!NetworkUtils.checkConnection(this) || blogId == 0) {
            return;
        }

        boolean isCurrentlyFollowed = isFollowedBlogId(blogId);
        final boolean isAskingToFollow = !isCurrentlyFollowed;

        ReaderActions.ActionListener actionListener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (isFinishing()) return;
                followButton.setEnabled(true);
                if (!succeeded) {
                    followButton.setIsFollowedAnimated(!isAskingToFollow);
                    int resId = (isAskingToFollow ? R.string.reader_toast_err_follow_blog : R.string.reader_toast_err_unfollow_blog);
                    ToastUtils.showToast(ReaderContactsActivity.this, resId);
                    if (isAskingToFollow) {
                        mFollowedBlogIds.remove(blogId);
                    } else {
                        mFollowedBlogIds.add(blogId);
                    }
                }
            }
        };

        ReaderBlogActions.followBlogById(blogId, isAskingToFollow, actionListener);
        followButton.setIsFollowedAnimated(isAskingToFollow);
        followButton.setEnabled(false);

        if (isAskingToFollow) {
            mFollowedBlogIds.add(blogId);
        } else {
            mFollowedBlogIds.remove(blogId);
        }
    }

    class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {
        private final ReaderUserList mUsers = new ReaderUserList();

        public UserAdapter() {
            setHasStableIds(true);
        }

        private void setUsers(@NonNull ReaderUserList users) {
            mUsers.clear();
            mUsers.addAll(users);
            notifyDataSetChanged();
        }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_listitem, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            ReaderUser user = getItem(position);

            holder.txtName.setText(user.getDisplayName());
            holder.txtUrl.setText(user.getUrlDomain());

            boolean isFollowing = isFollowedBlogId(user.blogId);
            holder.btnFollow.setIsFollowed(isFollowing);

            if (user.hasAvatarUrl()) {
                holder.imgAvatar.setImageUrl(
                        GravatarUtils.fixGravatarUrl(user.getAvatarUrl(), mAvatarSz),
                        WPNetworkImageView.ImageType.AVATAR);
            } else {
                holder.imgAvatar.showDefaultGravatarImageAndNullifyUrl();
            }
        }

        ReaderUser getItem(int position) {
            return mUsers.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mUsers.get(position).userId;
        }

        @Override
        public int getItemCount() {
            return mUsers.size();
        }
    }

    class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtName;
        private final TextView txtUrl;
        private final ReaderFollowButton btnFollow;
        private final WPNetworkImageView imgAvatar;

        public UserViewHolder(View view) {
            super(view);

            txtName = view.findViewById(R.id.text_name);
            txtUrl = view.findViewById(R.id.text_url);
            imgAvatar = view.findViewById(R.id.image_avatar);
            btnFollow = view.findViewById(R.id.follow_button);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        ReaderUser user = getAdapter().getItem(position);
                        ReaderActivityLauncher.showReaderBlogPreview(
                                v.getContext(),
                                user.blogId);
                    }
                }
            });

            btnFollow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition();
                    if (isValidPosition(position)) {
                        long blogId = getAdapter().getItem(position).blogId;
                        toggleFollow(btnFollow, blogId);
                    }
                }
            });
        }

        boolean isValidPosition(int position) {
            return position >= 0 && position < getAdapter().getItemCount();
        }
    }
}
