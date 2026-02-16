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
/*    */ public class TeaGameSessionManager
/*    */ {
/*    */   private static TeaGameSessionManager i;
/*    */   private final List<ISession> sessions;
/*    */   /*    */   
/*    */   public static TeaGameSessionManager gI() {
/* 17 */     if (i == null) {
/* 18 */       i = new TeaGameSessionManager();
/*    */     }
/* 20 */     return i;
/*    */   }
/*    */ 
/*    */ 
/*    */   /*    */ 
/*    */ 
/*    */   
/*    */   public TeaGameSessionManager() {
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

