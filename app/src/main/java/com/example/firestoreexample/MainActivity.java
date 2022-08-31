package com.example.firestoreexample;

import static android.content.ContentValues.TAG;
import static android.provider.AlarmClock.EXTRA_MESSAGE;

import static java.security.AccessController.getContext;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    private StorageReference mStorageRef;
    private String DocID, Titulo, Descripcion;
    private int Fecha;
    private String filename, fnam;

    private static final int CAMERA_PIC_REQUEST = 1111;
    private ImageView mImage;
    private RatingBar mRating;


    private static ArrayList<Member> mArrayList = new ArrayList<>(); //ArrayList de objetos de tipo Member
    private MemberAdapter mMembersAdapter;  //Referencia al adaptador definidio en MemberAdapter.java

    ListView notificationListView;

    private DocumentSnapshot lastResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationListView = (ListView) findViewById(R.id.lista);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mImage = (ImageView) findViewById(R.id.camarapic);


        //Un clic para actualizar algun elemento de la lista?
        notificationListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Member mycursor = (Member) parent.getItemAtPosition(position);

                RatingBar rat = (RatingBar) view.findViewById(R.id.rati);

                Map<String, Object> user = new HashMap<>();
                user.put("titulo", mycursor.getTitulo());
                user.put("descripcion", mycursor.getDescripcion());
                user.put("anio", rat.getRating());
                user.put("image", mycursor.getImage());

                Toast.makeText(getApplicationContext(), mycursor.getTitulo() + "   ", Toast.LENGTH_LONG).show();

                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").whereEqualTo("titulo", mycursor.getTitulo()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {


                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " => " + document.getData());
                            db.collection("users").document(document.getId()).update(user);
                            readElements(getCurrentFocus());
                        }
                    }
                });

            }

        });


        //Metodo para borrar un elemento cuando el usuario presione un click largo sobre algun item

        notificationListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Member mycursor = (Member) parent.getItemAtPosition(position);

                //Necesarios para actualizar el ListView despues de borrarlo
                ListView mMembersListView = (ListView) findViewById(R.id.lista);
                final ArrayList<Member> arrayList = new ArrayList<Member>();
                mMembersAdapter = new MemberAdapter(MainActivity.this, mArrayList);
                mMembersListView.setAdapter(mMembersAdapter);


                Toast.makeText(getApplicationContext(), mycursor.getTitulo() + "   ", Toast.LENGTH_LONG).show();
                //Borrar el elemento en Firestore
                DeleteData(mycursor.getTitulo());

                //Actualizar el listview
                mArrayList.remove(position);
                mMembersAdapter.notifyDataSetChanged();


                return false;
            }
        });


        //Evento para paginar resultados el ListView cada que se hace un Swipe
        final SwipeRefreshLayout pullToRefresh = findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // print("pull");

                pagination(getCurrentFocus());
                pullToRefresh.setRefreshing(false);

            }
        });

        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mImage.setVisibility(View.GONE);
            }
        });
        readElements(getCurrentFocus());

    }

    //Metodo para obtener la imagen de la camara y guardarla en storage
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_PIC_REQUEST) {


            //Obtener la fecha para usarla como nombre de archivo
            SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
            Date myDate = new Date();
            filename = timeStampFormat.format(myDate);


            //Decodificar la imagen que fue guardada en el directorio DCIM
            Bitmap thumbnail = BitmapFactory.decodeFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + "kit" + ".jpg");

            mImage.setVisibility(View.VISIBLE);

            //Rotar la imagen
            Matrix mat = new Matrix();
            mat.postRotate(90);  // Angulo deseado
            thumbnail = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), mat, true);
            mImage.setImageBitmap(thumbnail);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();

            //Subir la imagen a Firebase Storage
            fnam = uploadFiletoFirestore(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + "kit" + ".jpg", filename);

            //Generar el dialog box para etiquetar la imagen
            LayoutInflater li = LayoutInflater.from(this);
            View promptsView = li.inflate(R.layout.dialog_add, null);
            AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            // set prompts.xml to alertdialog builder
            alertDialogBuilder.setView(promptsView);

            final EditText dlgTitulo = (EditText) promptsView.findViewById(R.id.dlgTitulo);
            final EditText dlgDescripcion = (EditText) promptsView.findViewById(R.id.dlgDescripcion);
            final EditText dlgFecha = (EditText) promptsView.findViewById(R.id.dlgFecha);
            final RatingBar dlgRating = (RatingBar) promptsView.findViewById(R.id.rati);


            //Iniciar dialog box
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                    print(dlgTitulo.getText().toString());
                                    Titulo = dlgTitulo.getText().toString();
                                    Descripcion = dlgDescripcion.getText().toString();
                                    Fecha = (int) dlgRating.getRating();

                                    //Agregar valores y actualizar
                                    addElements();
                                    readElements(getCurrentFocus());

                                }
                            })
                    .setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });

            //Crear alerta
            AlertDialog alertDialog = alertDialogBuilder.create();

            //Mostrar dialog box
            alertDialog.show();


        }
    }

    private String uploadFiletoFirestore(String File, String remotefilename) {

        final String[] link = new String[1];
        Uri file = Uri.fromFile(new File(File));
        StorageReference sr = mStorageRef.child("images/" + remotefilename + ".jpg");
        UploadTask uploadTask = sr.putFile(file);

        //Registar observers para notificar si el upload falla
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.

                fnam = sr.toString(); //Obtener path tipo gs:// para la imagen guardada
            }
        });
        return link[0];
    }

    //Funcion para rotar una imagen al vuelo
    private byte[] rotateImage(byte[] data, int angle) {
        Log.d("labot_log_info", "CameraActivity: Inside rotateImage");
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, null);
        Matrix mat = new Matrix();
        mat.postRotate(angle);
        bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }


    public void takePicture(View v) {
        //Instrucciones necesarias para poder escribir en DCIM
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        //Path donde deseamos guardar la imagen
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getPath() + "/" + "kit" + ".jpg");
        Uri imageUri = Uri.fromFile(file);
        //Activar la camara
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri); //Guardar la imagen con la calidad necesaria

        startActivityForResult(intent, CAMERA_PIC_REQUEST);


    }

    public void addElements() {

        // mArrayList.clear();


        FirebaseFirestore db = FirebaseFirestore.getInstance();


        Map<String, Object> user = new HashMap<>();
        user.put("titulo", Titulo);
        user.put("descripcion", Descripcion);
        user.put("anio", Fecha);
        user.put("image", fnam);


        // Add a new document with a generated ID
        db.collection("users")
                .add(user)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());

                        DocID = documentReference.getId();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });


    }

    public void readElements(View v) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //By default elements are orded ascendenttly
        db.collection("users").orderBy("anio").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        if (documentSnapshots.isEmpty()) {
                            Log.d(TAG, "onSuccess: LIST EMPTY");

                            return;
                        } else {
                            // Convert the whole Query Snapshot to a list
                            // of objects directly! No need to fetch each
                            // document.
                            List<Member> types = documentSnapshots.toObjects(Member.class);


                            final ArrayAdapter<Member> aa;
                            final ArrayList<Member> arrayList = new ArrayList<Member>();

                            aa = new ArrayAdapter<Member>(MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    arrayList);


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mArrayList.clear();

                                    // Agregar valores a la arrayList
                                    mArrayList.addAll(types);
                                    //Relacionar ListView con el adaptador y la lista de elementos que se obtienen de la lectura
                                    ListView mMembersListView = (ListView) findViewById(R.id.lista);

                                    mMembersAdapter = new MemberAdapter(MainActivity.this, mArrayList);
                                    mMembersListView.setAdapter(mMembersAdapter);
                                    mMembersAdapter.notifyDataSetChanged();

                                    Log.d(TAG, "onSuccess: " + mArrayList);

                                }
                            });


                        }
                        int index = 0;
                        for (Member n : mArrayList) {

                            //    DocumentSnapshot documentSnapshot = documentSnapshots.getDocuments().get(index);
                            //     String documentID = documentSnapshot.getId();


                            System.out.println(String.valueOf(n.getAnio()) + " " + n.getTitulo() + " " + n.getDescripcion());
                            index++;
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error read document", e);
                    }
                });


    }

    public void Search(View v) {

        // get prompts.xml view
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_search, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText dlgTitulo = (EditText) promptsView.findViewById(R.id.dlgTitulo);
        final EditText dlgFecha = (EditText) promptsView.findViewById(R.id.dlgFecha);
        final RatingBar dlgRating = (RatingBar) promptsView.findViewById(R.id.rati);


        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                // edit text
                                print(dlgTitulo.getText().toString());
                                Titulo = dlgTitulo.getText().toString();
                                Fecha = (int) dlgRating.getRating();

                                //compQuery(getCurrentFocus());
                                lastResult = null;
                                pagination(getCurrentFocus());

                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        AlertDialog alertDialog = alertDialogBuilder.create();

        // show it
        alertDialog.show();


    }

    public void compQuery(View v) {


        mArrayList.clear();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        //By default elements are orded ascendenttly
            db.collection("users")
                    .whereEqualTo("titulo", Titulo)
                    .whereGreaterThanOrEqualTo("anio", Fecha)
                    .orderBy("anio", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        if (documentSnapshots.isEmpty()) {
                            Log.d(TAG, "onSuccess: LIST EMPTY");

                            return;
                        } else {
                            // Convert the whole Query Snapshot to a list
                            // of objects directly! No need to fetch each
                            // document.
                            List<Member> types = documentSnapshots.toObjects(Member.class);


                            final ArrayAdapter<Member> aa;
                            final ArrayList<Member> arrayList = new ArrayList<Member>();

                            aa = new ArrayAdapter<Member>(MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    arrayList);


                            // Agregar valores a la arrayList
                            mArrayList.addAll(types);
                            //Relacionar ListView con el adaptador y la lista de elementos que se obtienen de la lectura
                            ListView mMembersListView = (ListView) findViewById(R.id.lista);

                            mMembersAdapter = new MemberAdapter(MainActivity.this, mArrayList);
                            mMembersListView.setAdapter(mMembersAdapter);

                            Log.d(TAG, "onSuccess: " + mArrayList);

                            mMembersAdapter.notifyDataSetChanged();


                        }
                        int index = 0;
                        for (Member n : mArrayList) {

                            //    DocumentSnapshot documentSnapshot = documentSnapshots.getDocuments().get(index);
                            //     String documentID = documentSnapshot.getId();
                            System.out.println(String.valueOf(n.getAnio()) + " " + n.getTitulo() + " " + n.getDescripcion());


                            index++;
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error read document", e);
                    }
                });


    }


    public void pagination(View v) {

        Query query;
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (lastResult == null) {
            query = db.collection("users")
                    .whereEqualTo("titulo", Titulo)
                    .whereGreaterThanOrEqualTo("anio", Fecha)
                    .orderBy("anio", Query.Direction.ASCENDING)
                    .limit(3);

        } else {

            query = db.collection("users")
                    .whereEqualTo("titulo", Titulo)
                    .whereGreaterThanOrEqualTo("anio", Fecha)
                    .orderBy("anio", Query.Direction.ASCENDING)
                    .startAfter(lastResult)
                    .limit(3);

        }


        //By default elements are orded ascendenttly
        query.get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot documentSnapshots) {
                        if (documentSnapshots.isEmpty()) {
                            Log.d(TAG, "onSuccess: LIST EMPTY");

                            return;
                        } else {
                            // Convert the whole Query Snapshot to a list
                            // of objects directly! No need to fetch each
                            // document.
                            List<Member> types = documentSnapshots.toObjects(Member.class);


                            final ArrayAdapter<Member> aa;
                            final ArrayList<Member> arrayList = new ArrayList<Member>();

                            aa = new ArrayAdapter<Member>(MainActivity.this,
                                    android.R.layout.simple_list_item_1,
                                    arrayList);


                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mArrayList.clear();
                                    // Agregar valores a la arrayList
                                    mArrayList.addAll(types);
                                    //Relacionar ListView con el adaptador y la lista de elementos que se obtienen de la lectura
                                    ListView mMembersListView = (ListView) findViewById(R.id.lista);

                                    mMembersAdapter = new MemberAdapter(MainActivity.this, mArrayList);
                                    mMembersListView.setAdapter(mMembersAdapter);

                                    Log.d(TAG, "onSuccess: " + mArrayList);
                                    //mMembersAdapter.clear();
                                    mMembersAdapter.notifyDataSetChanged();
                                }
                            });

                        }
                        int index = 0;
                        for (Member n : mArrayList) {

                            //    DocumentSnapshot documentSnapshot = documentSnapshots.getDocuments().get(index);
                            //     String documentID = documentSnapshot.getId();
                            System.out.println(String.valueOf(n.getAnio()) + " " + n.getTitulo() + " " + n.getDescripcion());


                            index++;
                        }

                        if (documentSnapshots.size() > 0) {
                            lastResult = documentSnapshots.getDocuments().get(documentSnapshots.size() - 1);

                        }


                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error read document", e);
                    }
                });


    }

    public void DeleteData(String FirstName) {


        FirebaseFirestore db = FirebaseFirestore.getInstance();





        //By default elements are orded ascendenttly
        db.collection("users").whereEqualTo("titulo", FirstName).
                get().
                addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {

                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String documentID = documentSnapshot.getId();

                        db.collection("users").document(documentID).delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unused) {

                            }
                        });
                    }
                });
    }

    //Terminar la sesion
    public void logout(View v) {

        Intent intent = new Intent(getApplicationContext(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); //Lanzar nuevamente el login y terminar el la ejecucion del activity anterior
        startActivity(intent);
        FirebaseAuth.getInstance().signOut();
        finish();

    }

    private String UploadToFirestore(byte[] bb, String FileName) {
        final String[] link = new String[1];
        StorageReference sr = mStorageRef.child("images/" + FileName + ".jpg");
        sr.putBytes(bb).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                fnam = sr.toString();


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });
        return link[0];
    }

    public View DownloadFromStorage(String DocID) {
        ImageView imageView = new ImageView(this);

        StorageReference imageRef = mStorageRef.child("images/" + DocID + ".jpg");
        long mAXBYTES = 1024 * 1024;
        imageRef.getBytes(mAXBYTES).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                Bitmap bitmap;

                bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                imageView.setImageBitmap(bitmap);

                Bitmap bm = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

            }
        });

        return imageView;

    }

    private void print(String texto) {

        Toast.makeText(this, texto, Toast.LENGTH_LONG).show();
    }

}