package com.despertadorflash;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Small conservative Spanish glossary for frequent ML Kit labels. */
public final class ImageLabelEs {
 private static final Map<String,String> ES=new HashMap<>();
 static{
  ES.put("food","comida");ES.put("dish","plato");ES.put("fruit","fruta");
  ES.put("vegetable","verdura");ES.put("drink","bebida");ES.put("bottle","botella");
  ES.put("person","persona");ES.put("people","personas");
  ES.put("dog","perro");ES.put("cat","gato");ES.put("bird","ave");ES.put("animal","animal");
  ES.put("car","automóvil");ES.put("vehicle","vehículo");ES.put("bicycle","bicicleta");
  ES.put("traffic light","semáforo");ES.put("bus","colectivo");
  ES.put("building","edificio");ES.put("house","casa");ES.put("window","ventana");
  ES.put("door","puerta");ES.put("table","mesa");ES.put("chair","silla");
  ES.put("book","libro");ES.put("computer","computadora");ES.put("phone","teléfono");
  ES.put("screen","pantalla");ES.put("plant","planta");ES.put("tree","árbol");
  ES.put("flower","flor");ES.put("sky","cielo");ES.put("water","agua");
  ES.put("road","calle");ES.put("street","calle");ES.put("sign","cartel");
 }
 private ImageLabelEs(){}
 public static String name(String label){
  if(label==null)return "Objeto no especificado";
  String trimmed=label.trim();
  if(trimmed.isEmpty())return "Objeto no especificado";
  String translated=ES.get(trimmed.toLowerCase(Locale.ROOT));
  return translated==null?trimmed+" (etiqueta original)":translated;
 }
}
