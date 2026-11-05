package com.despertadorflash;

import java.util.Locale;

/** Conservative Spanish labels for YAMNet AudioSet classes.
 * An unidentified class is omitted rather than shown in English.
 */
public final class SoundLabelsEs {
 private SoundLabelsEs(){}
 public static String name(String raw){
  if(raw==null)return null;
  String s=raw.toLowerCase(Locale.ROOT);
  if(has(s,"doorbell","ding-dong"))return "POSIBLE TIMBRE DE PUERTA";
  if(has(s,"knock","door slam","sliding door"))return "POSIBLES GOLPES EN UNA PUERTA";
  if((s.contains("door")&&!s.contains("indoor"))||s.contains("drawer"))return "POSIBLE PUERTA O CAJÓN";
  if(has(s,"tap","thump","bang","thud","pounding","hammer","slam"))return "POSIBLES GOLPES O IMPACTOS";
  if(has(s,"speech","conversation","narration","whisper","babbling","child speech"))return "POSIBLE CONVERSACIÓN";
  if(has(s,"shout","scream","yell"))return "POSIBLES GRITOS";
  if(has(s,"laughter","giggle","chuckle"))return "POSIBLES RISAS";
  if(has(s,"baby","crying","sobbing"))return "POSIBLE LLANTO";
  if(has(s,"dog","bark","howl","whimper"))return "POSIBLE PERRO LADRANDO";
  if(has(s,"cat","meow","purr"))return "POSIBLE GATO";
  if(has(s,"bird","chirp","tweet","crow","rooster"))return "POSIBLES AVES";
  if(has(s,"siren"))return "POSIBLE SIRENA";
  if(has(s,"smoke detector","fire alarm"))return "POSIBLE ALARMA DE INCENDIO";
  if(has(s,"alarm","buzzer","beep","electronic beep"))return "POSIBLE ALARMA O PITIDO";
  if(has(s,"telephone","ringtone","phone"))return "POSIBLE TELÉFONO";
  if(has(s,"bell","chime"))return "POSIBLE CAMPANA";
  if(has(s,"footstep","walking","stomp","shuffle"))return "POSIBLES PASOS";
  if(has(s,"glass","shatter","crash"))return "POSIBLE ROTURA O IMPACTO";
  if(has(s,"explosion","firecracker","gunshot"))return "POSIBLE ESTALLIDO";
  if(has(s,"thunder"))return "POSIBLE TRUENO";
  if(has(s,"rain","water","splash","drip","pour","liquid"))return "POSIBLE AGUA";
  if(has(s,"wind"))return "POSIBLE VIENTO";
  if(has(s,"motorcycle"))return "POSIBLE MOTO";
  if(has(s,"aircraft","airplane","helicopter"))return "POSIBLE AERONAVE";
  if(has(s,"train","rail"))return "POSIBLE TREN";
  if(has(s,"horn","honk"))return "POSIBLE BOCINA";
  if(has(s,"traffic","engine","vehicle","car","truck","bus","idling"))return "POSIBLE VEHÍCULO";
  if(has(s,"music","singing","guitar","piano","violin","drum","instrument"))return "POSIBLE MÚSICA";
  if(has(s,"applause","clap"))return "POSIBLES APLAUSOS";
  if(has(s,"vacuum","blender","drill","machine","motor","printer","mechanism"))return "POSIBLE MÁQUINA";
  if(has(s,"typing","keyboard","typewriter"))return "POSIBLE TECLADO";
  if(has(s,"clock","tick"))return "POSIBLE RELOJ";
  if(has(s,"cough"))return "POSIBLE TOS";
  if(has(s,"sneeze"))return "POSIBLE ESTORNUDO";
  if(has(s,"breathing","snoring"))return "POSIBLE RESPIRACIÓN O RONQUIDO";
  if(has(s,"fire","crackle"))return "POSIBLE CREPITACIÓN";
  return null;
 }
 private static boolean has(String x,String... words){for(String w:words)if(x.contains(w))return true;return false;}
}
