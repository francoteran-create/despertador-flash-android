package com.despertadorflash;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Explicit MANUAL evaluation. No recorded sound, classifier probabilities,
 * transcripts, identities or timestamps are stored in this aggregate.
 * Percentages are ratios of rated trials, NEVER laboratory accuracy.
 */
public final class SoundValidationMetrics {
 public static final String[] KEYS={"timbre","puerta","perro","bocina",
  "sirena","voz","llanto","otros"};
 public static final String[] LABELS={"Timbre","Golpes / portazo","Mascotas / ladridos",
  "Bocina / tránsito","Sirena / alarma","Voz / conversación","Llanto / gritos","Otros"};
 private static final long MAX=1_000_000L;
 public static final class Count {
  public final long yes,no,missed;
  public Count(long yes,long no,long missed){
   this.yes=Math.max(0,Math.min(MAX,yes));
   this.no=Math.max(0,Math.min(MAX,no));
   this.missed=Math.max(0,Math.min(MAX,missed));
  }
  public long trials(){return yes+no+missed;}
  public long ratedDetections(){return yes+no;}
  public int confirmedRatio(){
   return ratedDetections()==0?-1:(int)Math.round(100d*yes/ratedDetections());
  }
  public int observedCoverage(){
   return yes+missed==0?-1:(int)Math.round(100d*yes/(yes+missed));
  }
 }
 private final LinkedHashMap<String,Count> counts=new LinkedHashMap<>();
 public SoundValidationMetrics(){
  for(String key:KEYS)counts.put(key,new Count(0,0,0));
 }
 public static String kindForSound(String text){
  if(text==null)return "otros";
  if(text.contains("TIMBRE")||text.contains("CAMPANA"))return "timbre";
  if(text.contains("PORTAZO")||text.contains("PUERTA")||
     text.contains("CAJÓN")||text.contains("GOLPE")||text.contains("IMPACTO"))return "puerta";
  if(text.contains("PERRO")||text.contains("LADRANDO")||
     text.contains("AULLIDO")||text.contains("GATO"))return "perro";
  if(text.contains("BOCINA")||text.contains("VEHÍCULO")||
     text.contains("MOTO")||text.contains("TREN"))return "bocina";
  if(text.contains("SIRENA")||text.contains("ALARMA")||
     text.contains("INCENDIO"))return "sirena";
  if(text.contains("VOZ")||text.contains("CONVERSACIÓN"))return "voz";
  if(text.contains("LLANTO")||text.contains("GRITO"))return "llanto";
  return "otros";
 }
 public static String displayName(String key){
  for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return LABELS[i];
  return "Otros";
 }
 public synchronized Count get(String kind){
  Count c=counts.get(kind);
  return c!=null?c:counts.get("otros");
 }
 public synchronized void set(String kind,long yes,long no,long missed){
  if(!counts.containsKey(kind))return;
  counts.put(kind,new Count(yes,no,missed));
 }
 public synchronized void add(String kind,boolean correct,boolean missing){
  if(!counts.containsKey(kind))kind="otros";
  Count previous=counts.get(kind);
  counts.put(kind,new Count(previous.yes+(!missing&&correct?1:0),
    previous.no+(!missing&&!correct?1:0),
    previous.missed+(missing?1:0)));
 }
 public synchronized long totalYes(){
  long sum=0;for(Count c:counts.values())sum+=c.yes;return sum;
 }
 public synchronized long totalNo(){
  long sum=0;for(Count c:counts.values())sum+=c.no;return sum;
 }
 public synchronized long totalMissed(){
  long sum=0;for(Count c:counts.values())sum+=c.missed;return sum;
 }
 public synchronized int confirmedRatio(){
  long yes=totalYes(),no=totalNo();
  return yes+no==0?-1:(int)Math.round(100d*yes/(yes+no));
 }
 public synchronized int observedCoverage(){
  long yes=totalYes(),missed=totalMissed();
  return yes+missed==0?-1:(int)Math.round(100d*yes/(yes+missed));
 }
 public synchronized Map<String,Count> snapshot(){
  return new LinkedHashMap<>(counts);
 }
 public synchronized String report(){
  StringBuilder b=new StringBuilder();
  b.append("EVALUACIÓN MANUAL · NO ES PRECISIÓN CIENTÍFICA\n")
   .append("Aciertos marcados: ").append(totalYes()).append("\n")
   .append("Alertas incorrectas marcadas: ").append(totalNo()).append("\n")
   .append("Sonidos no detectados marcados: ").append(totalMissed()).append("\n")
   .append("Coincidencia entre alertas evaluadas: ").append(pct(confirmedRatio())).append("\n")
   .append("Cobertura de pruebas marcadas: ").append(pct(observedCoverage())).append("\n");
  for(String key:KEYS){
   Count c=counts.get(key);
   if(c.trials()==0)continue;
   b.append(displayName(key)).append(": ")
    .append(c.yes).append(" aciertos, ")
    .append(c.no).append(" incorrectas, ")
    .append(c.missed).append(" omisiones\n");
  }
  b.append("Solo incluye casos que una persona decidió evaluar. ")
   .append("No permite estimar sensibilidad real sin pruebas controladas.");
  return b.toString();
 }
 private static String pct(int p){return p<0?"Sin datos":p+"%";}
}
