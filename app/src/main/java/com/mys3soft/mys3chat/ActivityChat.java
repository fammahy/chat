package com.mys3soft.mys3chat;



import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.NonNull;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;

import com.firebase.client.FirebaseError;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mys3soft.mys3chat.Models.Message;
import com.mys3soft.mys3chat.Models.StaticInfo;
import com.mys3soft.mys3chat.Models.User;
import com.mys3soft.mys3chat.Services.DataContext;
import com.mys3soft.mys3chat.Services.LocalUserService;
import com.mys3soft.mys3chat.Services.Tools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import hani.momanii.supernova_emoji_library.Actions.EmojIconActions;
import hani.momanii.supernova_emoji_library.Helper.EmojiconEditText;
import hani.momanii.supernova_emoji_library.Helper.EmojiconTextView;


public class ActivityChat extends AppCompatActivity{
    DataContext db = new DataContext(this, null, null, 1);
    EditText messageArea;
    ScrollView scrollView;
    LinearLayout layout;
    Firebase reference1, reference2, refNotMess, refFriend;
    User user;
    String friendEmail;
    Firebase refUser;
    private int pageNo = 2;
    boolean retrieving = false;
    private FloatingActionButton submit_btn;
    RelativeLayout rLayout; ImageView img;

    private ChildEventListener reference1Listener;
    private ChildEventListener refFriendListener;
    private String friendFullName = "";



    // get the Firebase  storage reference
    FirebaseStorage storage = FirebaseStorage.getInstance();
    StorageReference storageReference = storage.getReference();

    // Uri indicates, where the image will be picked from
    private Uri filePath; private String strfilePath="";

    Bitmap bitmap; String imgname;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarChatActivity);
        setSupportActionBar(toolbar);
        messageArea = (EditText) findViewById(R.id.et_Message);
        scrollView = (ScrollView) findViewById(R.id.scrollView);
        rLayout = (RelativeLayout) findViewById(R.id.rlOverlay);
        img = (ImageView) findViewById(R.id.imageView);
        //rLayout.setAlpha(0);
        layout = (LinearLayout) findViewById(R.id.layout1);
        user = LocalUserService.getLocalUserFromPreferences(this);
        Firebase.setAndroidContext(this);
        reference1Listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                if (!dataSnapshot.getKey().equals(StaticInfo.TypingStatus)) {
                    Map map = dataSnapshot.getValue(Map.class);
                    String mess = map.get("Message").toString();
                    String senderEmail = map.get("SenderEmail").toString();
                    String sentDate = map.get("SentDate").toString();
                    try {
                        // remove from server
                        reference1.child(dataSnapshot.getKey()).removeValue();
                        // save message on local db
                        db.saveMessageOnLocakDB(senderEmail, user.Email, mess, sentDate);
                        if (senderEmail.equals(user.Email)) {
                            // login user
                            appendMessage(mess, sentDate, 1, false,layout.getChildCount());
                        } else {
                            appendMessage(mess, sentDate, 2, false,layout.getChildCount());
                        }
                    } catch (Exception e) {

                    }
                } else {
                    // show typing status
                    String typingStatus = dataSnapshot.getValue().toString();
                    if (typingStatus.equals("Typing")) {
                        getSupportActionBar().setSubtitle(typingStatus + "...");
                    }
                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String typingStatus = dataSnapshot.getValue().toString();
                if (typingStatus.equals("Typing")) {
                    getSupportActionBar().setSubtitle(typingStatus + "...");
                } else {
                    // check if online
                    getSupportActionBar().setSubtitle("Online");
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                //layout.removeAllViews();
                if (dataSnapshot.getKey().equals("TypingStatus")) {
                    getSupportActionBar().setSubtitle("Online");

                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };
        refFriendListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getKey().equals("Status")) {
                    // check if subtitle is not Typing
                    CharSequence subTitle = getSupportActionBar().getSubtitle();
                    if (subTitle != null) {
                        if (!subTitle.equals("Typing...")) {
                            String friendStatus = dataSnapshot.getValue().toString();
                            if (!friendStatus.equals("Online")) {
                                friendStatus = Tools.lastSeenProper(friendStatus);
                            }
                            getSupportActionBar().setSubtitle(friendStatus);
                        }
                    } else {
                        String friendStatus = dataSnapshot.getValue().toString();
                        if (!friendStatus.equals("Online")) {
                            friendStatus = Tools.lastSeenProper(friendStatus);
                        }
                        getSupportActionBar().setSubtitle(friendStatus);
                    }


                }

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                String friendStatus = dataSnapshot.getValue().toString();
                if (!friendStatus.equals("Online")) {
                    friendStatus = Tools.lastSeenProper(friendStatus);
                }
                getSupportActionBar().setSubtitle(friendStatus);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {

            }
        };
        Bundle extras = getIntent().getExtras();
        friendEmail = extras.getString("FriendEmail");
        List<Message> chatList = db.getChat(user.Email, friendEmail, 1);
        for (Message item : chatList) {
            int messageType = item.FromMail.equals(user.Email) ? 1 : 2;
            appendMessage(item.Message, item.SentDate, messageType, false,layout.getChildCount());
        }

        friendFullName = extras.getString("FriendFullName");

        getSupportActionBar().setTitle(friendFullName);
        reference1 = new Firebase(StaticInfo.MessagesEndPoint + "/" + user.Email + "-@@-" + friendEmail);
        reference2 = new Firebase(StaticInfo.MessagesEndPoint + "/" + friendEmail + "-@@-" + user.Email);
        refFriend = new Firebase(StaticInfo.UsersURL + "/" + friendEmail);
        refNotMess = new Firebase(StaticInfo.NotificationEndPoint + "/" + friendEmail);
        refFriend.addChildEventListener(refFriendListener);
        StaticInfo.UserCurrentChatFriendEmail = friendEmail;
        refUser = new Firebase(StaticInfo.UsersURL + "/" + user.Email);
        submit_btn = (FloatingActionButton) findViewById(R.id.submit_btn);

        messageArea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (messageArea.getText().toString().length() == 0) {
                    reference2.child(StaticInfo.TypingStatus).setValue("");
                } else if (messageArea.getText().toString().length() == 1) {
                    reference2.child(StaticInfo.TypingStatus).setValue("Typing");
                    // change color here
                    //  submit_btn.setColorFilter(R.color.colorPrimary);

                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        View rootView = findViewById(R.id.rootLayout);
        EmojiconEditText emojiconEditText = (EmojiconEditText) findViewById(R.id.et_Message);
        ImageView emojiImageView = (ImageView) findViewById(R.id.emoji_btn);

        final EmojIconActions emojIcon = new EmojIconActions(this, rootView, emojiconEditText, emojiImageView, "#1c2764", "#e8e8e8", "#f4f4f4");
        emojIcon.ShowEmojIcon();

        emojIcon.setKeyboardListener(new EmojIconActions.KeyboardListener() {
            @Override
            public void onKeyboardOpen() {

                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }

            @Override
            public void onKeyboardClose() {

            }
        });

        final SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
              // new Runnable() { public void run(){
                    List<Message> chatList = db.getChat(user.Email, friendEmail, pageNo);
                     layout.removeAllViews(); int index=0;
                for(
                    Message item :chatList)

                    {
                       // if(retrieving){try{new Thread().wait(1000);}catch (Exception e){e.printStackTrace();}}
                        int messageType = item.FromMail.equals(user.Email) ? 1 : 2;
                        appendMessage(item.Message, item.SentDate, messageType, true,index);
                        index+=1;

                    }
                swipeRefreshLayout.setRefreshing(false);
                    pageNo++;
               // }};
            }
        });
        
        
       // getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityChat.this, ActivityFriendProfile.class);
                intent.putExtra("Email", friendEmail);
                startActivityForResult(intent, StaticInfo.ChatAciviityRequestCode);
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        Bundle extras = getIntent().getExtras();
        friendEmail = extras.getString("FriendEmail");

       // getSupportActionBar().setTitle(extras.getString("FriendFullName"));
       // getSupportActionBar().setIcon(R.drawable.dp_placeholder_sm);

        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        StaticInfo.UserCurrentChatFriendEmail = friendEmail;
        // update status to online
        refUser.child("Status").setValue("Online");
        reference1.addChildEventListener(reference1Listener);
    }


    @Override
    protected void onPause() {
        super.onPause();
        reference1.removeEventListener(reference1Listener);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        StaticInfo.UserCurrentChatFriendEmail = friendEmail;
        refUser.child("Status").setValue("Online");
    }

    @Override
    protected void onStop() {
        super.onStop();
        StaticInfo.UserCurrentChatFriendEmail = "";
        reference1.removeEventListener(reference1Listener);
        reference2.child(StaticInfo.TypingStatus).setValue("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        StaticInfo.UserCurrentChatFriendEmail = "";
        // set last seen
        DateFormat dateFormat = new SimpleDateFormat("dd MM yy hh:mm a");
        Date date = new Date();
        refUser.child("Status").setValue(dateFormat.format(date));
        reference1.removeEventListener(reference1Listener);
        reference2.child(StaticInfo.TypingStatus).setValue("");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Bundle extras = intent.getExtras();
        layout.removeAllViews(); int index=0;
        friendEmail = extras.getString("FriendEmail");
        friendFullName = extras.getString("FriendFullName");
        getSupportActionBar().setTitle(friendFullName);
        List<Message> chatList = db.getChat(user.Email, friendEmail, 1);
        for (Message item : chatList) {
            int messageType = item.FromMail.equals(user.Email) ? 1 : 2;
            appendMessage(item.Message, item.SentDate, messageType, false,index); index+=1;
        }

        StaticInfo.UserCurrentChatFriendEmail = friendEmail;
        reference1.removeEventListener(reference1Listener);
        reference1 = new Firebase(StaticInfo.MessagesEndPoint + "/" + user.Email + "-@@-" + friendEmail);
        reference1.addChildEventListener(reference1Listener);

        refFriend.removeEventListener(refFriendListener);
        refFriend = new Firebase(StaticInfo.UsersURL + "/" + friendEmail);
        refFriend.addChildEventListener(refFriendListener);

        reference2 = new Firebase(StaticInfo.MessagesEndPoint + "/" + friendEmail + "-@@-" + user.Email);

    }

    public void btn_SendMessageClick(View view) {

        String message = messageArea.getText().toString().trim();
        messageArea.setText("");
        if (!message.equals("")) {
            Map<String, String> map = new HashMap<>();
            map.put("Message", message);
            map.put("SenderEmail", user.Email);
            map.put("FirstName", user.FirstName);
            map.put("LastName", user.LastName);

            DateFormat dateFormat = new SimpleDateFormat("dd MM yy hh:mm a");
            Date date = new Date();
            String sentDate = dateFormat.format(date);

            map.put("SentDate", sentDate);
            //reference1.push().setValue(map);
            reference2.push().setValue(map);
            refNotMess.push().setValue(map);

            // save in local db
            db.saveMessageOnLocakDB(user.Email, friendEmail, message, sentDate);

            // appendmessage
            appendMessage(message, sentDate, 1, false, layout.getChildCount());

        }
    }

    //this function is called when a user clicks the camera icon from the chat page
    public void btn_SendImageClick(View view) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        photoPickerIntent.setType("image/*");
        photoPickerIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        photoPickerIntent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(photoPickerIntent, 10);


    }

    public void btn_closeImage(View view) {
        ScrollView layout = (ScrollView) findViewById(R.id.scrollView);
        layout.setVisibility(View.VISIBLE);
        LinearLayout layout2 = (LinearLayout)findViewById(R.id.layout2);
        layout2.setVisibility(View.INVISIBLE);



    }

    public void btn_Upload(View v){
       // final String rand =  UUID.randomUUID().toString();
        StorageReference ref = storageReference.child("images/"+imgname);
        ref.putFile(filePath)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                       // progressDialog.dismiss();
                        Toast.makeText(ActivityChat.this, "Photo Sent", Toast.LENGTH_SHORT).show();


                        String message = "<image>"+imgname+"</image>";
                        String local_message = "<image>"+imgname+"</image>";
                        Map<String, String> map = new HashMap<>();
                        map.put("Message", message);
                        map.put("SenderEmail", user.Email);
                        map.put("FirstName", user.FirstName);
                        map.put("LastName", user.LastName);

                        DateFormat dateFormat = new SimpleDateFormat("dd MM yy hh:mm a");
                        Date date = new Date();
                        String sentDate = dateFormat.format(date);

                        map.put("SentDate", sentDate);
                        //reference1.push().setValue(map);
                        reference2.push().setValue(map);
                        refNotMess.push().setValue(map);

                        // save in local db
                        db.saveMessageOnLocakDB(user.Email, friendEmail,local_message, sentDate);

                        // appendmessage
                        appendMessage(message, sentDate, 1, false, layout.getChildCount());
                        btn_closeImage(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                       // progressDialog.dismiss();
                        Toast.makeText(ActivityChat.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
                                .getTotalByteCount());
                        //progressDialog.setMessage("Uploaded "+(int)progress+"%");
                    }
                });
    }

    public void setImage(Bitmap bitmap){
        img = (ImageView) findViewById(R.id.imageView);
        img.setImageBitmap(bitmap);
        FileOutputStream foStream; imgname = UUID.randomUUID().toString();
        try {
            foStream = getApplicationContext().openFileOutput(imgname, Context.MODE_PRIVATE);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, foStream);
            foStream.close();
        } catch (Exception e) {
            Log.d("saveImage", "Exception 2, Something went wrong!");
            Toast.makeText(ActivityChat.this, e.toString(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void appendMessage(String mess, String sentDate, int messType, final boolean scrollUp, final int index) {

        EmojiconTextView textView = new EmojiconTextView(this);
        textView.setEmojiconSize(100);
        sentDate = Tools.messageSentDateProper(sentDate);
        SpannableString dateString = new SpannableString(sentDate);
        dateString.setSpan(new RelativeSizeSpan(0.7f), 0, sentDate.length(), 0);
        dateString.setSpan(new ForegroundColorSpan(Color.GRAY), 0, sentDate.length(), 0);

        if(!mess.contains("<image>")) {textView.setText(mess + "\n");}else{textView.setText("");}
        textView.append(dateString);
        //textView.setTextSize(15);
        textView.setTextColor(Color.parseColor("#000000"));

    //image text
        ImageView image = null;


        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                6f
        );
        // for image
        final LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
                600, 500,
                6f
        );
        lp.setMargins(0, 0, 0, 5);
        lp2.setMargins(0, 0, 0, 0);

        // 1 user
        if (messType == 1) {
            if(mess.contains("<image>")) {
                Log.d("image url: ",mess.substring(7,mess.length()-8));
                image = new ImageView(this);//image.setImageURI(android.net.Uri.parse(mess.substring(7,mess.length()-8)));
                Bitmap bitmap = null;
                FileInputStream fiStream;
                try {
                    fiStream    = getApplicationContext().openFileInput(mess.substring(7,mess.length()-8));
                    bitmap      = BitmapFactory.decodeStream(fiStream);
                    image.setImageBitmap(bitmap);
                    fiStream.close();
                } catch (Exception e) {
                    Log.d("saveImage", "Exception 3, Something went wrong!");
                    e.printStackTrace();
                }

            }
            textView.setBackgroundResource(R.drawable.messagebg1);
            lp.gravity = Gravity.RIGHT;
            lp2.gravity = Gravity.RIGHT;
        }
        //  2 friend
        else {
            textView.setBackgroundResource(R.drawable.messagebg2);
            lp.gravity = Gravity.LEFT;
            lp2.gravity = Gravity.LEFT;

            if(mess.contains("<image>")) {
                retrieving = true;
                //get the image from firebase storage
                StorageReference mImageRef =
                        FirebaseStorage.getInstance().getReference("images/"+mess.substring(7,mess.length()-8));
                final long ONE_MEGABYTE = 1024 * 1024;
                mImageRef.getBytes(ONE_MEGABYTE)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                DisplayMetrics dm = new DisplayMetrics();
                                getWindowManager().getDefaultDisplay().getMetrics(dm);
                                ImageView image = new ImageView(getApplicationContext());
                                image.setImageBitmap(bm);
                                image.setLayoutParams(lp2);
                                layout.addView(image,index);
                                retrieving = false;
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        retrieving = false;
                    }
                });

                //end of getting image
                //image = new ImageView(this);image.setImageURI(filePath);
            }

        }

        textView.setPadding(12, 4, 12, 4);

        textView.setLayoutParams(lp);


        if(image!=null){image.setLayoutParams(lp2);layout.addView(image,index);}

        else layout.addView(textView,index);
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                if (scrollUp)
                    scrollView.fullScroll(View.FOCUS_UP);
                else
                    scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_deleteConservation) {
            new AlertDialog.Builder(this)
                    .setTitle(friendFullName)
                    .setMessage("Are you sure to delete this chat?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            db.deleteChat(user.Email, friendEmail);
                            layout.removeAllViews();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        }
        if (id == R.id.menu_deleteContact) {
            new AlertDialog.Builder(this)
                    .setTitle(friendFullName)
                    .setMessage("Are you sure to delete this contact?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Firebase ref = new Firebase(StaticInfo.EndPoint + "/friends/" + user.Email + "/" + friendEmail);
                            ref.removeValue();
                            // delete from local database
                            db.deleteFriendByEmailFromLocalDB(friendEmail);
                            finish();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        }

        if (id == R.id.menu_friendProfile) {
            Intent intent = new Intent(ActivityChat.this, ActivityFriendProfile.class);
            intent.putExtra("Email", friendEmail);
            startActivityForResult(intent, StaticInfo.ChatAciviityRequestCode);
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == StaticInfo.ChatAciviityRequestCode && resultCode == Activity.RESULT_OK) {
            User updatedFriend = db.getFriendByEmailFromLocalDB(friendEmail);
            friendFullName = updatedFriend.FirstName;
            getSupportActionBar().setTitle(updatedFriend.FirstName);
        }

        if (requestCode == 10 && resultCode == Activity.RESULT_OK) {
            Uri selectedImage = data.getData();
            filePath = selectedImage;
            //strfilePath = RealPathUtil.getRealPathFromURI_API19(this,selectedImage);
            try {
                ScrollView layout = (ScrollView) findViewById(R.id.scrollView);
                layout.setVisibility(View.INVISIBLE);

                Bitmap bitmap = MediaStore.Images.Media.getBitmap(ActivityChat.this.getContentResolver(), selectedImage);
                setImage(bitmap);
                LinearLayout layout2 = (LinearLayout)findViewById(R.id.layout2);
                layout2.setVisibility(View.VISIBLE);

            } catch (IOException e) {
                Log.i("TAG", "Some exception " + e);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }







}






