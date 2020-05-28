package Lab7Client;


import Flat.Flat;

import java.io.Serializable;

/**
 * Класс - прототип команды, который будет отправлен на сервер для обработки.
 * Содержит поля:
 * commandName - имя команды
 * id - id элемента коллекции
 * element - объект коллекции
 * filename - имя файла для execute_script
 */
public class Command implements Serializable {

    private String commandName;
    private int id;
    private Flat element;
    private String filename;
    private String login;
    private String  address;
    private byte[] hashbytes;

    public void setAddress(String address){
        this.address = address;
    }
    public String getAddress() {
        return address;
    }
    public void setLogin(String login) {
        this.login = login;
    }

    public void setHashbytes(byte[] hashbytes) {
        this.hashbytes = hashbytes;
    }

    public String getLogin() {
        return login;
    }

    public byte[] getHashbytes() {
        return hashbytes;
    }

    private static final long serialVersionUID = 1L;

    public String getCommandName() {
        return commandName;
    }

    public void setCommandName(String commandName) {
        this.commandName = commandName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Flat getElement() {
        return element;
    }

    public void setElement(Flat element) {
        this.element = element;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }
}

