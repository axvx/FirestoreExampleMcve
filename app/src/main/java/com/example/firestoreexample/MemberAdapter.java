package com.example.firestoreexample;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.w3c.dom.Text;

import java.util.List;


public class MemberAdapter extends ArrayAdapter<Member> {

    //Variables para hacer referencia a Storage y al ImageView donde cargar la imagen
    StorageReference mStorageRef;
    ImageView imagenLista;


    public MemberAdapter(Context context, List<Member> object) {  //Recibir del contexto una lista de tipo Memeber
        super(context, 0, object);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //Referencias Firebase





        



        mStorageRef = FirebaseStorage.getInstance().getReference();
        FirebaseStorage storage = FirebaseStorage.getInstance();


        if (convertView == null) {

            //Generar una vista con 3 textviews pars mostrar los campos
            convertView = ((Activity) getContext()).getLayoutInflater().inflate(R.layout.item_member, parent, false);
        }

        //Declarar los textvies e ImageView y extraer campos de la interfaz
        TextView fechaTextView = (TextView) convertView.findViewById(R.id.txtfecha);
        TextView DescripcionTextView = (TextView) convertView.findViewById(R.id.txtDescripcion);
        TextView TituloTextView = (TextView) convertView.findViewById(R.id.txtTitulo);
        RatingBar ratingBar = (RatingBar) convertView.findViewById(R.id.rati);


        imagenLista = (ImageView) convertView.findViewById(R.id.imagen_lista);

        imagenLista.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toast.makeText(getContext(),"ss",Toast.LENGTH_LONG).show();
            }
        });


        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                //     Toast.makeText(getContext(),String.valueOf(rating),Toast.LENGTH_LONG).show();

            }
        });

        //Obtener la posicion
        Member member = getItem(position);

        //Mostrarlos
        fechaTextView.setText(member.getAnio().toString());
        DescripcionTextView.setText(member.getDescripcion());
        TituloTextView.setText(member.getTitulo());
        ratingBar.setRating(member.getAnio());

        //Mostrar y cargar imagenes de Storage usando Glide
        StorageReference storageRef = storage.getReferenceFromUrl(member.getImage());
        Glide.with(getContext()).load(storageRef).dontAnimate().into(imagenLista);


        return convertView;
    }


}