/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.mineshafter.proxy.handlers;

import info.mineshafter.util.Http;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Referenced classes of package info.mineshafter.proxy.handlers:
//            Handler

public class SkinHandler
    implements Handler
{

    public SkinHandler()
    {
        skinCache = new Hashtable();
        cloakCache = new Hashtable();
    }

    public void handle(String url, Map headers, byte body[], OutputStream res)
    {
        Matcher skinMatcher = SKIN_URL.matcher(url);
        Matcher cloakMatcher = CLOAK_URL.matcher(url);
        byte skindata[] = (byte[])null;
        if(skinMatcher.matches())
            skindata = handleSkin(skinMatcher.group(1));
        else
        if(cloakMatcher.matches())
            skindata = handleCloak(cloakMatcher.group(1));
        Http.sendResponse(res, "image/png", skindata);
    }

    public byte[] handleSkin(String username)
    {
        System.out.println("Proxy: Skin");
        if(!skinCache.containsKey(username))
        {
            String url = (new StringBuilder("http://")).append(SKIN_SERVER).append("/mcapi/skin/").append(username).append(".png").toString();
            System.out.println((new StringBuilder("To: ")).append(url).toString());
            byte skindata[] = Http.getRequest(url);
            skinCache.put(username, skindata);
        }
        return (byte[])skinCache.get(username);
    }

    public byte[] handleCloak(String username)
    {
        System.out.println("Proxy: Cloak");
        if(!cloakCache.containsKey(username))
        {
            String url = (new StringBuilder("http://")).append(SKIN_SERVER).append("/mcapi/cloak/").append(username).append(".png").toString();
            System.out.println((new StringBuilder("To: ")).append(url).toString());
            byte cloakdata[] = Http.getRequest(url);
            cloakCache.put(username, cloakdata);
        }
        return (byte[])cloakCache.get(username);
    }

    public static String SKIN_SERVER = "mineshafter.info";
    public static Pattern SKIN_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftSkins/(.+?)\\.png");
    public static Pattern CLOAK_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftCloaks/(.+?)\\.png");
    private Map skinCache;
    private Map cloakCache;

}

