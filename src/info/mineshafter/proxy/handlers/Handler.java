/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package info.mineshafter.proxy.handlers;

import java.io.OutputStream;
import java.util.Map;

public abstract interface Handler
{
  public abstract void handle(String paramString, Map<String, String> paramMap, byte[] paramArrayOfByte, OutputStream paramOutputStream);
}

