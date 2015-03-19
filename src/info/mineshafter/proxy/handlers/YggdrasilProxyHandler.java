/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.mineshafter.proxy.handlers;

import info.mineshafter.Util;
import info.mineshafter.proxy.HttpProxyHandler;
import info.mineshafter.proxy.SocksMessage;
import info.mineshafter.proxy.SocksProxyHandler;
import info.mineshafter.util.Http;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YggdrasilProxyHandler
  implements HttpProxyHandler, SocksProxyHandler
{
  public static String authServer = "mineshafter.info";
  private Map<Pattern, Handler> handlers = new HashMap();
  
  public YggdrasilProxyHandler()
  {
    Handler sh = new SkinHandler();
    this.handlers.put(SkinHandler.SKIN_URL, sh);
    this.handlers.put(SkinHandler.CLOAK_URL, sh);
    
    Handler imp = new YggdrasilImpersonator(new File(Util.getWorkingDirectory(), "launcher_profiles.json"));
    
    this.handlers.put(YggdrasilImpersonator.AUTHSERVER_URL, imp);
    
    Profiles p = new Profiles();
    this.handlers.put(Profiles.PROFILE_BY_ID, p);
    this.handlers.put(Profiles.PROFILE_BY_NAME, p);
  }
  
  public boolean onConnect(InputStream in, OutputStream out, SocksMessage msg)
  {
    in.mark(65535);
    String firstLine = Http.readUntil(in, '\n');
    String[] request = firstLine.split(" ");
    if (request.length != 3)
    {
      System.out.println("Not an HTTP request: " + firstLine);
      try
      {
        in.reset();
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
      return false;
    }
    String method = request[0].toUpperCase();
    String path = request[1];
    
    Map<String, String> headers = new HashMap();
    String header;
    do
    {
      header = Http.readUntil(in, '\n');
      header = header.trim();
      int splitPoint = header.indexOf(':');
      if (splitPoint != -1) {
        headers.put(header.substring(0, splitPoint).toLowerCase().trim(), header.substring(splitPoint + 1).trim());
      }
    } while (header.length() > 0);
    String url = "http://" + (String)headers.get("host") + path;
    System.out.println("Proxy - onConnect - " + method + " " + url);
    
    byte[] body = (byte[])null;
    if (method.equalsIgnoreCase("POST"))
    {
      int contentLength = Integer.parseInt((String)headers.get("content-length"));
      String b = Http.readUntil(in, contentLength);
      body = b.getBytes();
    }
    boolean r = handler(url, headers, body, out);
    try
    {
      in.reset();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return r;
  }
  
  public boolean handler(String url, Map<String, String> headers, byte[] body, OutputStream res)
  {
    for (Pattern p : this.handlers.keySet())
    {
      Matcher m = p.matcher(url);
      if (m.matches())
      {
        Handler h = (Handler)this.handlers.get(p);
        h.handle(url, headers, body, res);
        return true;
      }
    }
    return false;
  }
  
  public boolean onGET(String url, Map<String, String> headers, InputStream in, OutputStream out)
  {
    System.out.println("Proxy: GET " + url);
    
    return handler(url, headers, null, out);
  }
  
  public boolean onPOST(String url, Map<String, String> headers, InputStream in, OutputStream out)
  {
    System.out.println("Proxy: POST " + url);
    
    int contentLength = Integer.parseInt((String)headers.get("content-length"));
    String b = Http.readUntil(in, contentLength);
    byte[] body = b.getBytes();
    
    return handler(url, headers, body, out);
  }
  
  public boolean onHEAD(String url, Map<String, String> headers, InputStream in, OutputStream out)
  {
    System.out.println("Proxy: HEAD " + url);
    return false;
  }
  
  public boolean onCONNECT(String url, Map<String, String> headers, InputStream in, OutputStream out)
  {
    System.out.println("Proxy: CONNECT " + url);
    return false;
  }
  
  public boolean onBind()
  {
    return false;
  }
  
  class ProfileRequest
  {
    String name;
    String agent;
    
    public ProfileRequest(String name)
    {
      this.name = name;
      this.agent = "Minecraft";
    }
  }
}
