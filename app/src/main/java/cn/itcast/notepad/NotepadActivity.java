package cn.itcast.notepad;

import static android.webkit.ConsoleMessage.MessageLevel.LOG;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.nfc.Tag;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import cn.itcast.notepad.adapter.NotepadAdapter;
import cn.itcast.notepad.bean.NotepadBean;
import cn.itcast.notepad.database.SQLiteHelper;
import cn.itcast.notepad.utils.DBUtils;

public class NotepadActivity extends Activity {
    ListView listView;
    List<NotepadBean> list;
    SQLiteHelper mSQLiteHelper;
    NotepadAdapter adapter;
    EditText editText;
    TextView textView;
    Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //强行开启屏幕旋转效果
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
        if(savedInstanceState == null){
            setContentView(R.layout.activity_notepad);
        }
        if(savedInstanceState != null){
            //横屏
            if( ScreenOrient(this)==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE  )
                setContentView(R.layout.activity_notepad);
            //竖屏
            if( ScreenOrient(this)==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT  )
                setContentView(R.layout.activity2);
            String temp = savedInstanceState.getString("data_key") ;
//            Log.d(Tag, "重新创建了Activity，之前保存的内容是"+temp) ;
        }

//        setContentView(R.layout.activity_notepad);
        //用于显示便签的列表
        editText = (EditText) findViewById(R.id.editText);
        textView = (TextView) findViewById(R.id.textview);
        listView = (ListView) findViewById(R.id.listview);
        ImageView add = (ImageView) findViewById(R.id.add);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            //文本改变执行前
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            //文本改变执行中
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //如果长度为0
                if (charSequence.length() == 0) {
                    //隐藏“删除”图片
//                    ImageView.setVisibility(View.GONE);
                    showQueryData();
                } else {//长度不为0
                    //显示“删除图片”
//                    mImageView.setVisibility(View.VISIBLE);
                    //显示ListView
                    showListView();
                }
            }

            @Override
            //文本改变执行后
            public void afterTextChanged(Editable editable) { }
        });

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //如果输入框内容为空，提示请输入搜索内容
                if(TextUtils.isEmpty(editText.getText().toString().trim())){
                    Toast.makeText(NotepadActivity.this, "请输入您要搜索的内容", Toast.LENGTH_SHORT).show();
                }else {
                    //判断cursor是否为空
                    if (cursor != null) {
                        int columnCount = cursor.getCount();
                        if (columnCount == 0) {
                            Toast.makeText(NotepadActivity.this, "对不起，没有你要搜索的内容", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }

        });

        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotepadActivity.this,
                        RecordActivity.class);
                startActivityForResult(intent, 1);
            }
        });
        initData();
    }

    //判定当前的屏幕是竖屏还是横屏
    public int ScreenOrient(Activity activity)
    {
        int orient = activity.getRequestedOrientation();
        if(orient != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE && orient != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            WindowManager windowManager = activity.getWindowManager();
            Display display = windowManager.getDefaultDisplay();
            int screenWidth  = display.getWidth();
            int screenHeight = display.getHeight();
            orient = screenWidth < screenHeight ?  ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        }
        return orient;
    }

    protected void initData() {
        mSQLiteHelper= new SQLiteHelper(this); //创建数据库
        showQueryData();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent,View view,int position,long id){
                NotepadBean notepadBean = list.get(position);
                Intent intent = new Intent(NotepadActivity.this, RecordActivity.class);
                intent.putExtra("id", notepadBean.getId());
                intent.putExtra("time", notepadBean.getNotepadTime()); //记录的时间
                intent.putExtra("content", notepadBean.getNotepadContent()); //记录的内容
                NotepadActivity.this.startActivityForResult(intent, 1);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int
                    position, long id) {
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder( NotepadActivity.this)
                        .setMessage("是否删除此事件？")
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                NotepadBean notepadBean = list.get(position);
                                if(mSQLiteHelper.deleteData(notepadBean.getId())){
                                    list.remove(position);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(NotepadActivity.this,"删除成功",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                dialog =  builder.create();
                dialog.show();
                return true;
            }
        });

    }

    //模糊查询
    private void showListView() {
//        listView.setVisibility(View.VISIBLE);
        if(list!=null)
            list.clear();
        //获得输入的内容
        String str = editText.getText().toString().trim();
        //得到cursor
//        List<NotepadBean> list=new ArrayList<NotepadBean>();
        cursor = mSQLiteHelper.rawQuery("select * from Note where content like '%" + str + "%'");
        if (cursor!=null){
            while (cursor.moveToNext()){
                NotepadBean noteInfo=new NotepadBean();
                String id = String.valueOf(cursor.getInt
                        (cursor.getColumnIndex(DBUtils.NOTEPAD_ID)));
                String content = cursor.getString(cursor.getColumnIndex
                        (DBUtils.NOTEPAD_CONTENT));
                String time = cursor.getString(cursor.getColumnIndex
                        (DBUtils.NOTEPAD_TIME));
                noteInfo.setId(id);
                noteInfo.setNotepadContent(content);
                noteInfo.setNotepadTime(time);
                list.add(noteInfo);
            }
            cursor.close();
        }
        NotepadAdapter adapter = new NotepadAdapter(this, list);

        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //把cursor移动到指定行
                cursor.moveToPosition(position);
                String name = cursor.getString(cursor.getColumnIndex("name"));
                Toast.makeText(NotepadActivity.this,name, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQueryData(){
        if (list!=null){
            list.clear();
        }
        //从数据库中查询数据(保存的标签)
        list = mSQLiteHelper.query();
        adapter = new NotepadAdapter(this, list);
        listView.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode==1&&resultCode==2){
            showQueryData();
        }
    }
}
