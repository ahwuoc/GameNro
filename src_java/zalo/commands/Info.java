/**
 * Author: ENZEEFXNROxBARCOLL
 * Trao Đổi: https://zalo.me/g/mjevun948
 */
package zalo.commands;

import zalo.interfaces.Command;
import zalo.message.MessageContext;
import zalo.utils.Apis;
import zalo.models.ThreadType;
import zalo.apis.SendMessageApi;
import java.util.*;

public class Info implements Command {
    // ĐỊT MẸ CON CHÓ BÙI XUÂN NGHĨA CHỈ BIẾT NÚP ĐÉO DÁM 11
    // ĐỊT MẸ MẤY CON CHÓ BỢ ĐÍT BÙI XUÂN NGHĨA ĂN CỨT
    /* THẰNG LỒN NGHĨA CHỈ BIẾT VU OAN  CHỨ ĐÉO CHỨNG MINH ĐƯỢC HÀI VÃI LỒN HAHAHAHA
    1/ MỒM NÓI GWEN SPAM BOX NÓ NHƯNG KHI TÌM LẠI TIN NHẮN CHỈ CÓ 1 TIN NHẮN ???
    2/ BẢO ACC ĐỨC RYO ĐI SCAM NHƯNG TRONG KHI FB ĐẤY LẠI BỊ MẤY THẰNG BÊN NRO SCAM NGƯỢC ??????
    3/ MỒM NÓI 2K9 CHECK CCCD LẠI RA 2K2 MÀ LẠI KHAI ĐI HỌC 2K6
    4/ MỒM BẢO ĐÉO CHẤP NHƯNG TRONG KHI LẠI BỊ TAO CLEAR CẢ 2 3 LẦN PHẢI OUT BOX >?
    */

    
    @Override
    public String getName() {
        return "info";
    }
    
    @Override
    public String getDescription() {
        return "Hiển thị thông tin người dùng hoặc người được tag";
    }
    
    @Override
    public String getTag() {
        return "group";
    }
    
    @Override
    public int getCooldown() {
        return 0;
    }
    
    @Override
    public int getRole() {
        return 2;
    }
    
    @Override
    public void run(Command.CommandContext context) {
        try {
            MessageContext message = context.getMessage();
            Apis api = context.getApi();
            List<String> args = context.getArgs();
            
            String threadId = message.getThreadId();
            ThreadType threadType = message.getThreadType();
            
            String targetId = null;
            Map<String, Object> data = message.getData();
            
            Object mentionsObj = data.get("mentions");
            if (mentionsObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> mentions = (List<Map<String, Object>>) mentionsObj;
                if (!mentions.isEmpty()) {
                    Map<String, Object> firstMention = mentions.get(0);
                    Object uidObj = firstMention.get("uid");
                    if (uidObj == null) {
                        uidObj = firstMention.get("id");
                    }
                    if (uidObj != null) {
                        targetId = String.valueOf(uidObj);
                    }
                }
            }
            
            if (targetId == null && args != null && !args.isEmpty()) {
                String textArg = String.join(" ", args).trim();
                String digits = textArg.replaceAll("\\D", "");
                if (digits.matches("^\\d{9,15}$")) {
                    targetId = digits;
                } else {
                    targetId = args.get(0);
                }
            }
            
            if (targetId == null) {
                Object uidFrom = data.get("uidFrom");
                if (uidFrom == null) {
                    uidFrom = data.get("uid");
                }
                if (uidFrom != null) {
                    targetId = String.valueOf(uidFrom);
                }
            }
            
            if (targetId == null || targetId.isEmpty()) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Không thể xác định người dùng.");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            Map<String, Object> infoRes = api.getUserInfo.getUserInfo(targetId).get();
            
            if (infoRes == null || infoRes.isEmpty()) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Không tìm thấy thông tin người dùng (response rỗng).");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
             
             Object errorCodeObj = infoRes.get("error_code");
            if (errorCodeObj instanceof Number) {
                int errorCode = ((Number) errorCodeObj).intValue();
                if (errorCode != 0) {
                    String errorMessage = infoRes.containsKey("error_message") ? 
                        String.valueOf(infoRes.get("error_message")) : "Unknown error";
                  }
            }
            
            Map<String, Object> profile = null;
            
             Object changedProfilesObj = infoRes.get("changed_profiles");
            if (changedProfilesObj == null) {
                changedProfilesObj = infoRes.get("changedProfiles");
            }
            
            if (changedProfilesObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> changedProfiles = (Map<String, Object>) changedProfilesObj;
                 
                 @SuppressWarnings("unchecked")
                Map<String, Object> profileFromMap = (Map<String, Object>) changedProfiles.get(targetId);
                if (profileFromMap != null) {
                    profile = profileFromMap;
                } else {
                     String targetIdWithSuffix = targetId.contains("_") ? targetId : targetId + "_0";
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profileWithSuffix = (Map<String, Object>) changedProfiles.get(targetIdWithSuffix);
                    if (profileWithSuffix != null) {
                        profile = profileWithSuffix;
                    } else if (!changedProfiles.isEmpty()) {
                        // Lấy profile đầu tiên
                        @SuppressWarnings("unchecked")
                        Map<String, Object> firstProfile = (Map<String, Object>) changedProfiles.values().iterator().next();
                        profile = firstProfile;
                     }
                }
            }
            
             if (profile == null) {
                Object unchangedProfilesObj = infoRes.get("unchanged_profiles");
                if (unchangedProfilesObj == null) {
                    unchangedProfilesObj = infoRes.get("unchangedProfiles");
                }
                if (unchangedProfilesObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> unchangedProfiles = (Map<String, Object>) unchangedProfilesObj;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profileFromUnchanged = (Map<String, Object>) unchangedProfiles.get(targetId);
                    if (profileFromUnchanged != null) {
                        profile = profileFromUnchanged;
                    } else {
                        String targetIdWithSuffix = targetId.contains("_") ? targetId : targetId + "_0";
                        @SuppressWarnings("unchecked")
                        Map<String, Object> profileWithSuffix = (Map<String, Object>) unchangedProfiles.get(targetIdWithSuffix);
                        if (profileWithSuffix != null) {
                            profile = profileWithSuffix;
                        } else if (!unchangedProfiles.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> firstProfile = (Map<String, Object>) unchangedProfiles.values().iterator().next();
                            profile = firstProfile;
                         }
                    }
                }
            }
            
            if (profile == null) {
                Object dataObj = infoRes.get("data");
                
                if (dataObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dataMap = (Map<String, Object>) dataObj;
                    
                    Object dataChangedProfiles = dataMap.get("changed_profiles");
                    if (dataChangedProfiles == null) {
                        dataChangedProfiles = dataMap.get("changedProfiles");
                    }
                    if (dataChangedProfiles instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dataChanged = (Map<String, Object>) dataChangedProfiles;
                        if (!dataChanged.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> profileById = (Map<String, Object>) dataChanged.get(targetId);
                            if (profileById != null) {
                                profile = profileById;
                            } else {
                                String targetIdWithSuffix = targetId.contains("_") ? targetId : targetId + "_0";
                                @SuppressWarnings("unchecked")
                                Map<String, Object> profileWithSuffix = (Map<String, Object>) dataChanged.get(targetIdWithSuffix);
                                if (profileWithSuffix != null) {
                                    profile = profileWithSuffix;
                                } else {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> firstProfile = (Map<String, Object>) dataChanged.values().iterator().next();
                                    profile = firstProfile;
                                }
                            }
                        }
                    } else {
                        Object dataUnchangedProfiles = dataMap.get("unchanged_profiles");
                        if (dataUnchangedProfiles == null) {
                            dataUnchangedProfiles = dataMap.get("unchangedProfiles");
                        }
                        if (dataUnchangedProfiles instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> dataUnchanged = (Map<String, Object>) dataUnchangedProfiles;
                            if (!dataUnchanged.isEmpty()) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> firstProfile = (Map<String, Object>) dataUnchanged.values().iterator().next();
                                profile = firstProfile;
                            }
                        } else {
                            profile = dataMap;
                         }
                    }
                } else if (dataObj instanceof String) {
                    String dataStr = (String) dataObj;
                    try {
                        Object parsed = zalo.utils.Json.parse(dataStr);
                        if (parsed instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> parsedMap = (Map<String, Object>) parsed;
                            profile = parsedMap;
                        }
                    } catch (Exception e) {
                        try {
                            String decrypted = zalo.utils.Crypto.decodeAES(api.getContext().getSecretKey(), dataStr);
                            if (decrypted != null) {
                                Object parsed = zalo.utils.Json.parse(decrypted);
                                if (parsed instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> parsedMap = (Map<String, Object>) parsed;
                                    
                                    Object decryptedChangedProfiles = parsedMap.get("changed_profiles");
                                    if (decryptedChangedProfiles == null) {
                                        decryptedChangedProfiles = parsedMap.get("changedProfiles");
                                    }
                                    if (decryptedChangedProfiles instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> decryptedChanged = (Map<String, Object>) decryptedChangedProfiles;
                                         if (!decryptedChanged.isEmpty()) {
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> profileById = (Map<String, Object>) decryptedChanged.get(targetId);
                                            if (profileById != null) {
                                                profile = profileById;
                                            } else {
                                                String targetIdWithSuffix = targetId.contains("_") ? targetId : targetId + "_0";
                                                @SuppressWarnings("unchecked")
                                                Map<String, Object> profileWithSuffix = (Map<String, Object>) decryptedChanged.get(targetIdWithSuffix);
                                                if (profileWithSuffix != null) {
                                                    profile = profileWithSuffix;
                                                } else {
                                                    @SuppressWarnings("unchecked")
                                                    Map<String, Object> firstProfile = (Map<String, Object>) decryptedChanged.values().iterator().next();
                                                    profile = firstProfile;
                                                 }
                                            }
                                        }
                                    } else {
                                        profile = parsedMap;
                                    }
                                }
                            } else {
                            }
                        } catch (Exception e2) {
                        }
                    }
                } else if (dataObj == null) {
                    profile = infoRes;
                }
            }
            
            if (profile == null || profile.isEmpty()) {
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Không tìm thấy thông tin người dùng trong response.");
                msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
                api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
                return;
            }
            
            
            String userId = getStringValue(profile, "userId", "uid", targetId);
            String displayName = getStringValue(profile, "displayName", "zaloName", "name", "Unknown");
            String avatarUrl = getStringValue(profile, "avatar", "avatarUrl", "photo", null);
            
            Object genderObj = profile.get("gender");
            int genderCode = -1;
            if (genderObj instanceof Number) {
                genderCode = ((Number) genderObj).intValue();
            }
            String genderText = (genderCode == 0) ? "Nam" : (genderCode == 1) ? "Nữ" : "Không rõ";
            
            String birthday = getStringValue(profile, "sdob", "birthday", "dob", "Không rõ");
            
            String phone = getStringValue(profile, "phoneNumber", "phone", "Không rõ");
            
            String createdTs = "Không rõ";
            Object createdTsObj = profile.get("createdTs");
            if (createdTsObj instanceof Number) {
                long timestamp = ((Number) createdTsObj).longValue();
                if (timestamp < 10000000000L) {
                    timestamp *= 1000;
                }
                Date date = new Date(timestamp);
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
                createdTs = sdf.format(date);
            }
            
            List<String> lines = new ArrayList<>();
            lines.add("═══════════════════");
            lines.add(" THÔNG TIN NGƯỜI DÙNG");
            lines.add("═══════════════════");
            lines.add(" Tên: " + displayName);
            lines.add("UID: " + userId);
            lines.add("️  Giới tính: " + genderText);
            lines.add(" Sinh nhật: " + birthday);
            lines.add(" Số điện thoại: " + phone);
            lines.add(" Tạo tài khoản: " + createdTs);
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                lines.add("️  Avatar: " + avatarUrl);
            }
            lines.add("═══════════════════");
            
            SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
            msgContent.setMsg(String.join("\n", lines));
            msgContent.setQuote(SendMessageApi.createQuoteFromData(data));
            
            api.sendMessage.sendMessage(msgContent, threadId, threadType).get();
            
        } catch (Exception e) {
            System.err.println("[INFO] Error: " + e.getMessage());
            e.printStackTrace();
            try {
                MessageContext message = context.getMessage();
                Apis api = context.getApi();
                SendMessageApi.MessageContent msgContent = new SendMessageApi.MessageContent();
                msgContent.setMsg("Đã xảy ra lỗi khi lấy thông tin người dùng: " + e.getMessage());
                msgContent.setQuote(SendMessageApi.createQuoteFromData(message.getData()));
                api.sendMessage.sendMessage(msgContent, message.getThreadId(), message.getThreadType()).get();
            } catch (Exception e2) {
                System.err.println("[INFO] Error sending error message: " + e2.getMessage());
            }
        }
    }
    
    private String getStringValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}

