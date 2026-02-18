/*    */ package network.server;
/*    */ 
/*    */ import network.session.ISession;
/*    */ import java.util.ArrayList;
/*    */ import java.util.List;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class SessionManager_network
/*    */ {
/*    */   private static SessionManager_network i;
/*    */   private final List<ISession> sessions;
/*    */   /*    */   
/*    */   public static SessionManager_network gI() {
/* 17 */     if (i == null) {
/* 18 */       i = new SessionManager_network();
/*    */     }
/* 20 */     return i;
/*    */   }
/*    */ 
/*    */ 
/*    */   /*    */ 
/*    */ 
/*    */   
/*    */   public SessionManager_network() {
/* 26 */     this.sessions = new ArrayList<>();
/*    */   }
/*    */ 
/*    */   
/*    */   public void putSession(ISession session) {
/* 31 */     this.sessions.add(session);
/*    */   }
/*    */ 
/*    */   
/*    */   public void removeSession(ISession session) {
/* 36 */     this.sessions.remove(session);
/*    */   }
/*    */   
/*    */   public List<ISession> getSessions() {
/* 40 */     return this.sessions;
/*    */   }
/*    */   
/*    */   public ISession findByID(long id) throws Exception {
/* 44 */     if (this.sessions.isEmpty()) {
/* 45 */       throw new Exception("Session " + id + " không tồn tại");
/*    */     }
/* 47 */     for (ISession session : this.sessions) {
/* 48 */       if (session.getID() > id) {
/* 49 */         throw new Exception("Session " + id + " không tồn tại");
/*    */       }
/* 51 */       if (session.getID() == id) {
/* 52 */         return session;
/*    */       }
/*    */     } 
/* 55 */     throw new Exception("Session " + id + " không tồn tại");
/*    */   }
/*    */   
/*    */   public int getNumSession() {
/* 59 */     return this.sessions.size();
/*    */   }
/*    */ }


/* Location:              C:\Users\VoHoangKiet\Downloads\TEA_V5\lib\GirlkunNetwork.jar!\com\girlkun\network\server\SessionManager_network.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */