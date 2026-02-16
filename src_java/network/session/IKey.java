package network.session;

import network.io.Message;

public interface IKey {
  void sendKey() throws Exception;
  
  void setKey(Message paramMessage) throws Exception;
  
  void setKey(byte[] paramArrayOfbyte);
  
  byte[] getKey();
  
  boolean sentKey();
  
  void setSentKey(boolean paramBoolean);
}
