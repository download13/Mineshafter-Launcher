/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.mineshafter.proxy.handlers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import info.mineshafter.Util;
import info.mineshafter.util.Http;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

public class Profiles
  implements Handler
{
  public static Pattern PROFILE_BY_NAME = Pattern.compile("http://api.mojang.com/profiles/page/(.+?)");
  public static Pattern PROFILE_BY_ID = Pattern.compile("http://sessionserver.mojang.com/session/minecraft/profile/(.+?)");
  private Map<String, String> idToName = new Hashtable();
  
  @Override
  public void handle(String url, Map<String, String> headers, byte[] body, OutputStream res)
  {
    Matcher lookupMatcher = PROFILE_BY_NAME.matcher(url);
    Matcher idMatcher = PROFILE_BY_ID.matcher(url);
    
    String r = "";
    String stringBody = null;
    try
    {
      stringBody = new String(body, "UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      e.printStackTrace();
    }
    if (lookupMatcher.matches()) {
      r = handleLookup(lookupMatcher.group(1), stringBody);
    } else if (idMatcher.matches()) {
      r = handleGetProfile(idMatcher.group(1));
    }
    Http.sendResponse(res, "application/json", r);
  }
  
  private String handleGetProfile(String id)
  {
    System.out.println("Proxy: ProfileId");
    
    String name = (String)this.idToName.get(id);
    String skinServer = SkinHandler.SKIN_SERVER;
    
    String textureJSON = "{\"timestamp\":" + System.currentTimeMillis() + ",\"profileId\":\"" + id + "\",\"profileName\":\"" + name + 
      "\",\"isPublic\":true,\"textures\": {\"SKIN\": {\"url\":\"http://" + skinServer + "/mcapi/skin/" + name + ".png\"},\"CLOAK\": {\"url\":\"http://" + skinServer + "/mcapi/cloak/" + 
      name + ".png\"}}}";
    textureJSON = Base64.encodeBase64String(StringUtils.getBytesUtf8(textureJSON));
    String response = "{\"id\": \"" + id + "\", \"name\":\"download13\", \"properties\": [{\"name\": \"textures\", \"value\":\"" + textureJSON + "\", \"signature\": \"\"}]}";
    
    return response;
  }
  
  private String handleLookup(String page, String body)
  {
    System.out.println("Proxy: ProfileName");
    
    Gson gson = new Gson();
    

    Type t = new TypeToken() {}.getType();
    List<YggdrasilProxyHandler.ProfileRequest> profiles = (List)gson.fromJson(body, t);
    
    String name = ((YggdrasilProxyHandler.ProfileRequest)profiles.get(0)).name;
    String id = Util.getMd5(name);
    
    this.idToName.put(id, name);
    
    String response = "{\"profiles\":[{\"id\":\"" + id + "\",\"name\":\"" + name + "\"}],\"size\":1}";
    
    return response;
  }
}
