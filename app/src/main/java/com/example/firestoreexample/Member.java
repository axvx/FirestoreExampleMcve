package com.example.firestoreexample;

public class Member {
    private Long anio;
    private String titulo;
    private String descripcion;
    private String image;


    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }


    public Long getAnio() {
        return anio;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Member(Long anio, String titulo, String descripcion, String image) {
        this.anio = anio;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.image = image;
    }

    public Member() {

    }


}