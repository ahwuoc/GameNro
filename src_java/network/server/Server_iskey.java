package network.server;

public interface Server_iskey extends Runnable {
  Server_iskey init();
  
  Server_iskey start(int paramInt) throws Exception;
  
  Server_iskey setAcceptHandler(ISessionAcceptHandler paramISessionAcceptHandler);
  
  Server_iskey close();
  
  Server_iskey dispose();
  
  Server_iskey randomKey(boolean paramBoolean);
  
  Server_iskey setDoSomeThingWhenClose(IServerClose paramIServerClose);
  
  Server_iskey setTypeSessioClone(Class paramClass) throws Exception;
  
  ISessionAcceptHandler getAcceptHandler() throws Exception;
  
  boolean isRandomKey();
  
  void stopConnect();
}


/* Location:              C:\Users\VoHoangKiet\Downloads\TEA_V5\lib\GirlkunNetwork.jar!\com\girlkun\network\server\Server_iskey.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */