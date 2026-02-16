package network.server;

public interface InDucProServer extends Runnable {
  InDucProServer init();
  
  InDucProServer start(int paramInt) throws Exception;
  
  InDucProServer setAcceptHandler(ISessionAcceptHandler paramISessionAcceptHandler);
  
  InDucProServer close();
  
  InDucProServer dispose();
  
  InDucProServer randomKey(boolean paramBoolean);
  
  InDucProServer setDoSomeThingWhenClose(IServerClose paramIServerClose);
  
  InDucProServer setTypeSessioClone(Class paramClass) throws Exception;
  
  ISessionAcceptHandler getAcceptHandler() throws Exception;
  
  boolean isRandomKey();
  
  void stopConnect();
}

