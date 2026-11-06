package com.despertadorflash;
import org.junit.Test;
import static org.junit.Assert.*;
public class ImageLabelEsTest {
 @Test public void translatesKnownCommonCategories(){
  assertEquals("perro",ImageLabelEs.name("Dog"));
  assertEquals("semáforo",ImageLabelEs.name("TRAFFIC LIGHT"));
  assertEquals("teléfono",ImageLabelEs.name("Phone"));
 }
 @Test public void doesNotGuessUnknownObjects(){
  assertEquals("unknown_label (etiqueta original)",ImageLabelEs.name("unknown_label"));
 }
}
