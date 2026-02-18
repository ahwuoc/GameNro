package nro.services;

import jdbc.daos.PlayerDAO;
import nro.player.Player;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import utils.Util;

public class NapThe {

    private static NapThe I;

    public static NapThe gI() {
        if (NapThe.I == null) {
            NapThe.I = new NapThe();
        }
        return NapThe.I;
    }

    public void napThe(Player pl, String maThe, String seri) {
        System.out.println(maThe);
        System.out.println(seri);
    }

    public static final void SendCard(Player p, String loaiThe, String menhGia, String soSeri, String maPin) {

        String partnerId = "74807781872";
        String partnerKey = "c523fb692598e4611059df0cd1d13578";

        // sign = MD5(partner_key + code + serial)
        String sign = MD5Hash(partnerKey + maPin + soSeri);

        int requestID = Util.nextInt(100000000, 999999999);
        String requestIdStr = String.valueOf(requestID);

        try {
            OkHttpClient client = new OkHttpClient();

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("telco", loaiThe)
                    .addFormDataPart("code", maPin)
                    .addFormDataPart("serial", soSeri)
                    .addFormDataPart("amount", menhGia)
                    .addFormDataPart("request_id", requestIdStr)
                    .addFormDataPart("partner_id", partnerId)
                    .addFormDataPart("sign", sign)
                    .addFormDataPart("command", "charging")
                    .build();

            Request request = new Request.Builder()
                    .url("https://thesieure.com/chargingws/v2")
                    .post(body)
                    .build();

            okhttp3.Response response = client.newCall(request).execute();
            String jsonString = response.body().string();

            JSONObject json = (JSONObject) JSONValue.parse(jsonString);
            long status = (long) json.get("status");

            // ====== HANDLE STATUS ======
            if (status == 99 || status == 1) {
                // nạp thành công
                PlayerDAO.LogNapTIen(p.getSession().uu, menhGia, soSeri, maPin, requestIdStr);

                Service.gI().sendThongBaoOK(p,
                        "Gửi thẻ thành công\n"
                        + "Seri : " + soSeri + "\n"
                        + "Mã thẻ : " + maPin + "\n"
                        + "Mệnh giá : " + menhGia + "\n"
                        + "Time : " + java.time.LocalDate.now() + " " + java.time.LocalTime.now() + "\n"
                        + "Vui lòng thoát game để update lại số tiền.");
            }
            else if (status == 2) {
                Service.gI().sendThongBao(p, 
                        "Nạp thành công nhưng sai mệnh giá! Không được cộng tiền.");
            }
            else if (status == 3) {
                Service.gI().sendThongBao(p, 
                        "Bạn đã nhập sai thông tin thẻ.");
            }
            else if (status == 4) {
                Service.gI().sendThongBao(p, 
                        "Hệ thống nạp bảo trì.");
            }
            else if (status == 100) {
                Service.gI().sendThongBao(p, 
                        "Sai seri hoặc mã pin.");
            }

            System.out.println("STATUS: " + status + " | MENH GIA: " + menhGia
                    + " | SERIAL: " + soSeri + " | PIN: " + maPin);

        } catch (Exception e) {
            e.printStackTrace();
            Service.gI().sendThongBao(p, "Lỗi hệ thống nạp thẻ!");
        }
    }

    public static String MD5Hash(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] array = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : array) {
                sb.append(Integer.toHexString((b & 0xFF) | 0x100).substring(1, 3));
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
