/*    */ package network.session;
/*    */ 
/*    */ import java.net.Socket;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class SessionFactory
/*    */ {
/*    */   private static SessionFactory I;
/*    */   
/*    */   public static SessionFactory gI() {
/* 15 */     if (I == null) {
/* 16 */       I = new SessionFactory();
/*    */     }
/* 18 */     return I;
/*    */   }
/*    */ 
/*    */   
/*    */   public ISession cloneSession(Class<ISession> clazz, Socket socket) throws Exception {
/* 23 */     return clazz.getConstructor(new Class[] { Socket.class }).newInstance(new Object[] { socket });
/*    */   }
/*    */ }


