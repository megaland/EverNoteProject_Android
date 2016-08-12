package com.usnschool.evernoteproject;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    boolean overwrite = false;
    NoteView noteview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        noteview = new NoteView(this);
        setContentView(noteview);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,1,0,"저장하기");
        menu.add(0,2,0,"불러오기");
        menu.add(0,3,0,"업로드");
        menu.add(0,4,0,"다운로드");
        menu.add(0,5,0,"새로하기");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MySocket mysocket;
        switch(item.getItemId()){
            case 1 :

                if(!overwrite){
                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    final EditText textview = new EditText(this);
                    alert.setView(textview);
                    alert.setPositiveButton("저장", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            noteview.insertDB(textview.getText().toString());
                            Toast.makeText(MainActivity.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });
                    alert.show();


                }else{
                    noteview.updateDB();
                    Toast.makeText(MainActivity.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                }

                break;
            case 2 :
                Cursor cursor = noteview.getList();
                ArrayList<String> num = new ArrayList<String>();
                ArrayList<String> title = new ArrayList<String>();
                ArrayList<String> date = new ArrayList<String>();
                while(cursor.moveToNext()){
                    num.add(String.valueOf(cursor.getInt(0)));
                    title.add(cursor.getString(2));
                    date.add(cursor.getString(3));
                }
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                ListView listview = new ListView(this);
                MyAdapter adapter = new MyAdapter(this, R.layout.layout, num, title, date);
                listview.setAdapter(adapter);
                final ArrayList<String> numf = num;

                listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        noteview.showNote(numf.get(i));
                        Toast.makeText(MainActivity.this, ""+numf.get(i), Toast.LENGTH_SHORT).show();
                    }
                });
                alert.setPositiveButton("확인", null);
                alert.setView(listview);
                alert.show();
                overwrite = true;

                break;
            case 3:
                mysocket = new MySocket(noteview.sendSocket2(), 1);
                mysocket.start();
                break;
            case 4:
                mysocket = new MySocket(noteview.sendSocket2(), 2);
                mysocket.start();
                break;
            case 5:
                overwrite = false;
                noteview.arrayClear();
                noteview.postInvalidate();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
}

class NoteView extends View {
    static DBConnector dbconnector;
    ArrayList<LineContext> arrlines = new ArrayList<LineContext>();
    ArrayList arr;
    int currentnum;


    public NoteView(Context context) {
        super(context);
        dbconnector = new DBConnector(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setStrokeWidth(4);
        int x1 = 0;
        int x2 = 0;
        int y1 = 0;
        int y2 = 0;

        for (int i = 0; i < arrlines.size(); i++) {
            if (arrlines.get(i).state) {
                x1 = arrlines.get(i - 1).x;
                y1 = arrlines.get(i - 1).y;
                x2 = arrlines.get(i).x;
                y2 = arrlines.get(i).y;
                canvas.drawLine(x1, y1, x2, y2, paint);
            }
        }


    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int x = 0;
        int y = 0;
        boolean state = true;

        if(event.getAction()==MotionEvent.ACTION_DOWN){
            x = (int)event.getX();
            y = (int)event.getY();
            state = false;


        }else{
            x = (int)event.getX();
            y = (int)event.getY();
            state = true;

        }
        LineContext lcxt = new LineContext(x, y, state);
        arrlines.add(lcxt);
        invalidate();

        return true;
    }

    public void insertDB(String title){
        arr = new ArrayList();
        arr.add(arrlines);
        //사각형, 글자모음 등등 더 생기면 추가한다.
        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos;
        try {

            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(arr);


        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] buffer = baos.toByteArray();
        SQLiteDatabase db = dbconnector.getWritableDatabase();
        ContentValues values = new ContentValues();
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        values.put("data",buffer);
        values.put("title", title);
        values.put("date", sdf.format(date));
        db.insertOrThrow("EverNoteTBL", null, values);

    }
    public void showNote(String i){
        SQLiteDatabase db = dbconnector.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from evernotetbl where num = "+i, null);
        cursor.moveToNext();

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(cursor.getBlob(1));
            ObjectInputStream ois = new ObjectInputStream(bais);

            arr = (ArrayList)(ois.readObject());
            arrlines =(ArrayList<LineContext>) arr.get(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        currentnum = Integer.parseInt(i);
        invalidate();
    }

    public void updateDB(){

        ByteArrayOutputStream baos = null;
        ObjectOutputStream oos;
        try {

            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);
            oos.writeObject(arr);


        } catch (IOException e) {
            e.printStackTrace();
        }

        byte[] buffer = baos.toByteArray();
        SQLiteDatabase db = dbconnector.getWritableDatabase();
        ContentValues values = new ContentValues();
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        values.put("data",buffer);
        values.put("date", sdf.format(date));
        db.update("evernotetbl", values, "num = "+ getCurrentnum(), null);
    }

    public HashMap<String, Object> sendSocket(){//앞으로 쓰지않을 함수
        Cursor cursor = getList();
        HashMap<String, Object> hm = new HashMap<String, Object>();
        ArrayList<Integer> num = new ArrayList<Integer>();
        ArrayList<String> title = new ArrayList<String>();
        ArrayList<String> date = new ArrayList<String>();
        ArrayList<Object> data = new ArrayList<Object>();


        while(cursor.moveToNext()){
            try {

                ByteArrayInputStream bais = new ByteArrayInputStream(cursor.getBlob(1));
                ObjectInputStream ois = new ObjectInputStream(bais);
                num.add(cursor.getInt(0));
                data.add(ois.readObject());
                title.add(cursor.getString(2));
                date.add(cursor.getString(3));

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        hm.put("num", num);
        hm.put("data", data);
        hm.put("title", title);
        hm.put("date", date);

        return hm;
    }

    public ArrayList sendSocket2(){
        Cursor cursor = getList();
        ArrayList arrsend = new ArrayList();
        ArrayList<Integer> num = new ArrayList<Integer>();
        ArrayList<String> title = new ArrayList<String>();
        ArrayList<String> date = new ArrayList<String>();
        ArrayList data = new ArrayList();


        while(cursor.moveToNext()){
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(cursor.getBlob(1));
                ObjectInputStream ois = new ObjectInputStream(bais);
                num.add(cursor.getInt(0));
                data.add(ois.readObject());
                title.add(cursor.getString(2));
                date.add(cursor.getString(3));

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        arrsend.add(num);
        arrsend.add(data);
        arrsend.add(title);
        arrsend.add(date);

        return arrsend;
    }

    public void arrayClear(){
        arrlines.clear();
    }
    public int getCurrentnum(){
        return currentnum;
    }

    public Cursor getList(){
        SQLiteDatabase db = dbconnector.getReadableDatabase();
        Cursor cursor = db.rawQuery("select * from evernotetbl", null);
        return cursor;
    }

    static DBConnector getDbconnector(){
        return dbconnector;
    }



}

class LineContext implements Serializable{
    static final long serialVersionUID = 333333L;
    int x;
    int y;
    boolean state;

    public LineContext(int x, int y, boolean state) {
        this.x = x;
        this.y = y;
        this.state = state;
    }
}

class DBConnector extends SQLiteOpenHelper{

    public DBConnector(Context context) {
        super(context, "EverNoteDB.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "create table EverNoteTBL (num integer primary key autoincrement, data blob, title text, date text)";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "drop table IF EXISTS EverNoteTBL";
        db.execSQL(sql);
        onCreate(db);
    }
}

class MyAdapter extends BaseAdapter{
    Context ctx;
    ArrayList<String> num;
    ArrayList<String> title;
    ArrayList<String> date;
    int layout;
    LayoutInflater inflater;

    public MyAdapter(Context ctx, int layout, ArrayList<String> num,  ArrayList<String> title, ArrayList<String> date) {
        this.ctx = ctx;
        this.num = num;
        this.layout = layout;
        this.title = title;
        this.date = date;
        inflater = (LayoutInflater)ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return num.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if(view == null){
            view = inflater.inflate(layout, viewGroup, false);
        }

        TextView textview1 = (TextView)view.findViewById(R.id.textView);
        TextView textview2 = (TextView)view.findViewById(R.id.textView2);
        TextView textview3 = (TextView)view.findViewById(R.id.textView3);

        textview1.setText(num.get(i));
        textview2.setText(title.get(i));
        textview3.setText(date.get(i));

        return view;
    }
}

class MySocket extends Thread{
    Socket socket;
    HashMap<String, Object> hm;
    int protocol;
    ArrayList arrsend;
    public MySocket(HashMap<String, Object> hm, int protocol) {
        this.hm = hm;
        this.protocol = protocol;

    }
    public MySocket(ArrayList arrsend, int protocol){
        this.arrsend = arrsend;
        this.protocol = protocol;
    }

    @Override
    public void run() {
        try {
            Log.e("스타트","ㄷㄷㄷㄷㄷㄷㄷㄷㄷㄷ");
            socket = new Socket("192.168.1.11", 7777);
            if(protocol == 1){
                Log.i("접속되었ㅆ브니다", "ㅈㅈㅈㅈㅈㅈㅈㅈㅈ");
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeUTF("1");
                oos.flush();
                oos.writeObject(arrsend);
                oos.flush();
                Log.i("성공", "성공성공");
            }else if (protocol ==2){
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeUTF("2");
                oos.flush();

                InputStream is = socket.getInputStream();
                ObjectInputStream ois = new ObjectInputStream(is);
                //hm = (HashMap<String, Object>)ois.readObject();
                arrsend = (ArrayList)ois.readObject();
                Log.e("wwwwwwwwww", ""+arrsend.size());
                DBConnector connector = NoteView.getDbconnector();
                SQLiteDatabase db = connector.getWritableDatabase();
                db.delete("EverNoteTBL", null, null);
/*                ArrayList<Integer> num = (ArrayList<Integer>)(hm.get("num"));
                ArrayList data = ((ArrayList)(hm.get("data")));
                ArrayList<String> title = (ArrayList<String>)(hm.get("title"));
                ArrayList<String> date = (ArrayList<String>)(hm.get("date"));*/
                ArrayList<Integer> num = (ArrayList<Integer>)arrsend.get(0);
                ArrayList data = ((ArrayList)(arrsend.get(1)));
                ArrayList<String> title = (ArrayList<String>)arrsend.get(2);
                ArrayList<String> date = (ArrayList<String>)arrsend.get(3);

                for (int i = 0; i < num.size(); i++) {
                    ByteArrayOutputStream baos = null;
                    ObjectOutputStream oos2;
                    try {

                        baos = new ByteArrayOutputStream();
                        oos2 = new ObjectOutputStream(baos);
                        oos2.writeObject(data.get(i));



                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    ContentValues values = new ContentValues();
                    values.put("data", baos.toByteArray());
                    values.put("title", title.get(i));
                    values.put("date", date.get(i));
                    db.insertOrThrow("EverNoteTBL", null, values);
                }
                Log.e("다운로드 완료","did");

            }



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}