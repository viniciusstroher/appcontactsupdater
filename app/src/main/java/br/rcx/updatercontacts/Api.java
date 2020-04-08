package br.rcx.updatercontacts;

import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Api {
    //trata mensagens que chegam da UpdaterService
    public static JSONObject handleCommand(ContentResolver ctx,JSONObject objMessage) throws JSONException, OperationApplicationException, RemoteException {
        JSONObject returnObject = new JSONObject();
        String messageReturn = "esta acao nao existe";

        if(objMessage == null || ctx == null) {
            //json invalido retorna msg de error
            returnObject.put("message", messageReturn);
        }else {
            //json valido valida no switch
            String contactId = null;
            //addMessageToList("ação: " + objMessage.getString("action"));
            if(!objMessage.has("action")) {
                returnObject.put("message", messageReturn);
            }else if(!objMessage.has("phone")){
                messageReturn = "Parametro phone faltando";
                returnObject.put("message", messageReturn);
            }else {
                if(objMessage.getString("phone").equals("") || objMessage.isNull("phone")) {
                    messageReturn = "Parametro phone não pode ser vazio";
                    returnObject.put("message", messageReturn);
                }else {
                    //proura por ação
                    switch (objMessage.getString("action")) {
                        case "add_contact":
                            //procura contato
                            contactId = ContactService.getContactIdByNumber(ctx, objMessage.getString("phone"));

                            //verifica se contato existe
                            if (contactId != null) {
                                messageReturn = "Numero: " + objMessage.getString("phone") + " já existe";
                                returnObject.put("message", messageReturn);
                            } else {
                                String groupId = "6";
                                if (objMessage.has("groupid")) {
                                    groupId = objMessage.getString("groupid");
                                }

                                boolean replace = false;
                                if (objMessage.has("replace")) {
                                    replace = objMessage.getString("replace").equals("0") ? false : true;
                                }

                                //adiciona contato aos contatos do android
                                ContactService.addContact(ctx, objMessage.getString("phone"), groupId, replace);

                                messageReturn = "Adicionado numero: " + objMessage.getString("phone");
                                returnObject.put("message", messageReturn);
                            }
                            break;

                        case "get_contact":
                            contactId = ContactService.getContactIdByNumber(ctx, objMessage.getString("phone"));

                            if (contactId == null) {
                                returnObject.put("message", "contato não encontrado");
                            } else {
                                String contactName = ContactService.getContactDisplayNameByNumber(ctx, objMessage.getString("phone"));
                                String hasWhats = ContactService.hasWhatsapp(ctx, contactId);
                                JSONArray phoneNumbers = ContactService.getPhoneNumbers(ctx, contactId);
                                returnObject.put("message", "contato encontrado");
                                returnObject.put("id", contactId);
                                returnObject.put("contactId", contactId);
                                returnObject.put("contactName", contactName);
                                returnObject.put("phoneNumber", phoneNumbers);
                                returnObject.put("hasWhats", hasWhats == null ? "0" : "1");
                                returnObject.put("group", ContactService.getGroupIdFor(ctx, Long.parseLong(contactId)));
                            }
                            break;

                        case "remove_contact":

                            contactId = ContactService.getContactIdByNumber(ctx, objMessage.getString("phone"));
                            if (contactId == null) {
                                returnObject.put("message", "contato não encontrado");
                            } else {
                                ContactService.deleteContactById(ctx, contactId);
                                returnObject.put("message", "contato " + objMessage.getString("phone") + " removido");
                            }
                            break;

                        case "check_contact":

                            //pesquisa contato
                            contactId = ContactService.getContactIdByNumber(ctx, objMessage.getString("phone"));

                            if (contactId == null) {
                                String groupId = "6";
                                if (objMessage.has("groupid")) {
                                    groupId = objMessage.getString("groupid");
                                }

                                boolean replace = false;
                                if (objMessage.has("replace")) {
                                    replace = objMessage.getString("replace").equals("0") ? false : true;
                                }

                                //adiciona contato
                                ContactService.addContact(ctx, objMessage.getString("phone"), groupId, replace);
                                messageReturn = "Adicionado numero: " + objMessage.getString("phone");
                            }

                            //procura por contato
                            contactId = ContactService.getContactIdByNumber(ctx, objMessage.getString("phone"));

                            String contactName = ContactService.getContactDisplayNameByNumber(ctx, objMessage.getString("phone"));
                            String hasWhats = ContactService.hasWhatsapp(ctx, contactId);

                            JSONArray phoneNumbers = ContactService.getPhoneNumbers(ctx, contactId);
                            returnObject.put("message", "contato encontrado");
                            returnObject.put("id", contactId);
                            returnObject.put("contactId", contactId);
                            returnObject.put("contactName", contactName);
                            returnObject.put("phoneNumber", phoneNumbers);
                            returnObject.put("hasWhats", hasWhats == null ? "0" : "1");
                            returnObject.put("group", ContactService.getGroupIdFor(ctx, Long.parseLong(contactId)));

                            break;
                        default:
                            returnObject.put("message", messageReturn);
                            break;
                    }
                }
            }
        }
        return returnObject;
    }


    public static JSONObject checkPhone(ContentResolver ctx,String phone) throws JSONException, OperationApplicationException, RemoteException {
        String contactId = ContactService.getContactIdByNumber(ctx, phone);

        if (contactId == null) {
            String groupId = "6";
            boolean replace = false;
            //adiciona contato
            ContactService.addContact(ctx, phone, groupId, replace);
        }

        //procura por contato
        contactId = ContactService.getContactIdByNumber(ctx, phone);

        String contactName = ContactService.getContactDisplayNameByNumber(ctx, phone);
        String hasWhats = ContactService.hasWhatsapp(ctx, contactId);

        JSONArray phoneNumbers = ContactService.getPhoneNumbers(ctx, contactId);

        JSONObject returnObject = new JSONObject();
        returnObject.put("message", "contato encontrado");
        returnObject.put("id", contactId);
        returnObject.put("contactId", contactId);
        returnObject.put("contactName", contactName);
        returnObject.put("phoneNumber", phoneNumbers);
        returnObject.put("hasWhats", hasWhats == null ? "0" : "1");
        returnObject.put("group", ContactService.getGroupIdFor(ctx, Long.parseLong(contactId)));

        return returnObject;
    }
}
