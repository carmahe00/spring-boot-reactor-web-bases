package com.springboot.reactor.app.models;

import java.util.ArrayList;
import java.util.List;

public class Comentarios {

	private List<String> comentarios;

	public Comentarios() {
		comentarios = new ArrayList<String>();
	}

	public void addComentarios(String comentario) {
		this.comentarios.add(comentario);
	}

	@Override
	public String toString() {
		return "comentarios= " + comentarios ;
	}
	
}
