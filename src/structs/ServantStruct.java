/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package structs;

/**
 *
 * @author Martin
 */
public class ServantStruct {
    public String guid;
    public String parentId;
    public String[] files;
    public boolean isOnline;
    
    public ServantStruct(String guid)
    {
        this.guid = guid;
        isOnline = true;
    }
    public ServantStruct(String guid, String parentId)
    {
        this.guid = guid;
        this.parentId = parentId;
        isOnline = true;
    }
    public ServantStruct(String guid, String parentId, String files)
    {
        this.guid = guid;
        this.parentId = parentId;
        isOnline = true;
        this.files = files.split(",");
    }
    
    public boolean checkIfHasFile(String filename) {
        for (int i = 0; i < files.length; i++) {
            if (files[i].equals(filename)) {
                return true;
            }
        }
        return false;
    }
}
