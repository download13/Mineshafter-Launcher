/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.mineshafter.proxy.handlers;

import com.google.gson.Gson;
import info.mineshafter.Util;
import info.mineshafter.util.Http;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

public class YggdrasilMiddleman
  implements Handler
{
  public static Pattern AUTHSERVER_URL = Pattern.compile("http://authserver\\.mojang\\.com/(.*)");
  private List<Profile> profiles;
  
  public YggdrasilMiddleman(File profilesFile)
  {
    this.profiles = Collections.synchronizedList(new ArrayList());
    Gson gson = new Gson();
    try
    {
      ProfilesJSON profiles = (ProfilesJSON)gson.fromJson(new FileReader(profilesFile), ProfilesJSON.class);
      Map<String, Profile> ps = profiles.authenticationDatabase;
      for (String name : ps.keySet())
      {
        Profile p = (Profile)ps.get(name);
        if (p != null)
        {
          if ((p.displayName == null) || (p.displayName.length() == 0)) {
            p.displayName = p.username;
          }
          this.profiles.add(p);
        }
      }
    }
    catch (FileNotFoundException e) {}catch (NullPointerException e) {}
  }
  
  public void handle(String url, Map<String, String> headers, byte[] body, OutputStream res)
  {
    System.out.println("Proxy: Authserver");
    
    Matcher authServerMatcher = AUTHSERVER_URL.matcher(url);
    authServerMatcher.matches();
    String endpoint = authServerMatcher.group(1);
    
    Gson gson = new Gson();
    String b = null;
    try
    {
      b = new String(body, "UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      e.printStackTrace();
    }
    YggdrasilRequest data = (YggdrasilRequest)gson.fromJson(b, YggdrasilRequest.class);
    
    String response = "";
    System.out.println("Proxy postedJSON: " + b);
    if (endpoint.equalsIgnoreCase("authenticate"))
    {
      System.out.println("Proxy: authenticate");
      response = authenticate(b);
      System.out.println("Proxy authenticate response: " + response);
    }
    else if (endpoint.equalsIgnoreCase("refresh"))
    {
      System.out.println("Proxy: refresh");
      response = refresh(data);
      System.out.println("Proxy refresh response: " + response);
    }
    else if (endpoint.equalsIgnoreCase("invalidate"))
    {
      System.out.println("Proxy: invalidate");
      response = invalidate(data);
    }
    Http.sendResponse(res, "application/json", response);
  }
  
  public Profile getProfileByUsername(String name)
  {
    synchronized (this.profiles)
    {
      for (Profile p : this.profiles) {
        if (p.username.equals(name)) {
          return p;
        }
      }
    }
    return null;
  }
  
  public Profile getProfileByAccessToken(String token)
  {
    synchronized (this.profiles)
    {
      for (Profile p : this.profiles) {
        if (p.accessToken.equals(token)) {
          return p;
        }
      }
    }
    return null;
  }
  
  public String authenticate(YggdrasilRequest req)
  {
    Gson gson = new Gson();
    String accessToken = Util.getMd5(req.username + req.password + Long.toString(System.currentTimeMillis()));
    Profile p = getProfileByUsername(req.username);
    if (p == null)
    {
      String id = Util.getMd5(req.username);
      p = new Profile(req.username, accessToken, id, req.username);
      this.profiles.add(p);
    }
    p.accessToken = accessToken;
    
    ProfileResponse pr = new ProfileResponse(p.uuid, p.displayName);
    YggdrasilAuthResponse r = new YggdrasilAuthResponse(req.clientToken, p.accessToken, pr, pr);
    return gson.toJson(r);
  }
  
  public String authenticate(String body)
  {
    System.out.println("Req: " + body);
    
    URL u = null;
    try
    {
      u = new URL("https://authserver.mojang.com/authenticate");
    }
    catch (MalformedURLException e1)
    {
      e1.printStackTrace();
    }
    HttpsURLConnection c = null;
    byte[] response = (byte[])null;
    try
    {
      c = (HttpsURLConnection)u.openConnection(Proxy.NO_PROXY);
      c.setRequestProperty("Content-Type", "application/json");
      c.setDoOutput(true);
      c.getOutputStream().write(body.getBytes());
      response = Http.grabData(c.getInputStream());
    }
    catch (IOException e1)
    {
      try
      {
        if (c.getResponseCode() == 403)
        {
          response = Http.grabData(c.getErrorStream());
        }
        else
        {
          e1.printStackTrace();
          return "";
        }
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    String res = "";
    try
    {
      res = new String(response, "UTF-8");
    }
    catch (UnsupportedEncodingException e)
    {
      e.printStackTrace();
    }
    System.out.println("Res: " + res);
    
    return res;
  }
  
  public String refresh(YggdrasilRequest req)
  {
    Gson gson = new Gson();
    Profile p = getProfileByAccessToken(req.accessToken);
    if (p == null) {
      return "{\"error\":\"ForbiddenOperationException\",\"errorMessage\":\"Invalid token.\"}";
    }
    String newAccessToken = Util.getMd5(req.accessToken + Long.toString(System.currentTimeMillis()));
    p.accessToken = newAccessToken;
    YggdrasilAuthResponse r = new YggdrasilAuthResponse(req.clientToken, newAccessToken, new ProfileResponse(p.uuid, p.displayName), null);
    return gson.toJson(r);
  }
  
  public String invalidate(YggdrasilRequest req)
  {
    Profile p = getProfileByAccessToken(req.accessToken);
    p.accessToken = null;
    return "";
  }

    @Override
    public void handle(String s, Map map, byte[] abyte0, OutputStream outputstream) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
  
  public class Profile
  {
    public String username;
    public String accessToken;
    public String uuid;
    public String displayName;
    public String name;
    public String playerUUID;
    
    public Profile(String u, String t, String id, String d)
    {
      this.username = u;
      this.accessToken = t;
      this.uuid = id;
      this.displayName = d;
    }
  }
  
  public class ProfileResponse
  {
    public String id;
    public String name;
    
    public ProfileResponse(String id, String name)
    {
      this.id = id;
      this.name = name;
    }
  }
  
  public class ProfilesJSON
  {
    public Map<String, YggdrasilMiddleman.Profile> profiles;
    public String selectedProfile;
    public String clientToken;
    public Map<String, YggdrasilMiddleman.Profile> authenticationDatabase;
    
    public ProfilesJSON() {}
  }
  
  public class YggdrasilAuthResponse
  {
    public String accessToken;
    public String clientToken;
    public YggdrasilMiddleman.ProfileResponse selectedProfile;
    public ArrayList<YggdrasilMiddleman.ProfileResponse> availableProfiles;
    
    public YggdrasilAuthResponse(String clientToken, String accessToken, YggdrasilMiddleman.ProfileResponse selected, YggdrasilMiddleman.ProfileResponse available)
    {
      this.clientToken = clientToken;
      this.accessToken = accessToken;
      this.selectedProfile = selected;
      if (available != null)
      {
        this.availableProfiles = new ArrayList();
        this.availableProfiles.add(available);
      }
      else
      {
        this.availableProfiles = null;
      }
    }
  }
}
