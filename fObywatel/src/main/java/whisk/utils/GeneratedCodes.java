package whisk.utils;

import org.json.JSONObject;

import java.util.*;

import static whisk.Main.formatDate;

public class GeneratedCodes {

    private List<Code> codes = new ArrayList<>();

    public void addCode(String qrCode, String code){
        Code codeObject = new Code(qrCode, code);
        codes.add(codeObject);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                codes.remove(codeObject);
                cancel();
            }
        }, 300000);
    }

    public void setScanned(int cardId, String code, String qrCode, String seriesAndNumber, String validFrom, String validTo, String pesel){
        for (Code codeObject : codes){
            if (codeObject.code.equalsIgnoreCase(code) || codeObject.qrCode.equalsIgnoreCase(qrCode)) {
                codeObject.setScanned(cardId, seriesAndNumber, validFrom, validTo, pesel);
                break;
            }
        }
    }

    public void deleteCode(String qrCode){
        Code foundCode = null;

        for (Code codeObject : codes){
            if (codeObject.qrCode.equalsIgnoreCase(qrCode)) {
                foundCode = codeObject;
                break;
            }
        }

        if (foundCode != null){
            codes.remove(foundCode);
        }
    }

    public boolean checkCode(String qrCode, String code){
        boolean found = false;

        for (Code codeObject : codes){
            if (codeObject.code.equalsIgnoreCase(code) || codeObject.qrCode.equalsIgnoreCase(qrCode)){
                found = true;
                break;
            }
        }

        return found;
    }

    public JSONObject checkScanned(String qrCode){
        JSONObject object = null;

        for (Code codeObject : codes) {
            int scanned = codeObject.getScanned();
            if (codeObject.qrCode.equalsIgnoreCase(qrCode) && scanned != 0){
                object = new JSONObject();

                object.put("id", scanned);
                object.put("mobileIdCardNumber", codeObject.seriesAndNumber);
                object.put("mobileIdCardValidTo", formatDate(codeObject.validTo));
                object.put("mobileIdCardValidFrom", formatDate(codeObject.validFrom));
                object.put("pesel", codeObject.pesel);
                break;
            }
        }

        return object;
    }

    public class Code {

        private int scannerId = 0;
        private String code;
        private String qrCode;
        private String seriesAndNumber;
        private String validTo;
        private String validFrom;
        private String pesel;

        public Code(String qrCode, String code){
            this.code = code;
            this.qrCode = qrCode;
        }

        public void setScanned(int scannerId, String seriesAndNumber, String validFrom, String validTo, String pesel){
            this.scannerId = scannerId;
            this.seriesAndNumber = seriesAndNumber;
            this.validFrom = validFrom;
            this.validTo = validTo;
            this.pesel = pesel;
        }

        public int getScanned(){
            return scannerId;
        }

    }

}
