package Commands;

import Database.*;
import Flat.*;
import Lab7Client.Command;
import Lab7Server.CommandManager;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.JsonUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class CommandBase {
    private Date date;
    private static final Logger LOG = LogManager.getLogger();
    private CommandManager commandManager;
    private LinkedList<String> commandQueue = new LinkedList<>();
    private Database database = new Database();
    private PriorityQueue<Flat> p;
    private ConcurrentLinkedQueue<StringBuilder> answerQueue = new ConcurrentLinkedQueue<>();
    private ArrayList<String> commandList = new ArrayList<>();
    private Lock readLock;
    private Lock writeLock;
    private HashMap<String, ArrayList<Integer>> objectsOwners;
    private HashSet<String> paths = new HashSet<>();
    private String previousFile = "";
    private HashMap<String, String> users = new HashMap<>();
    private boolean scriptFlag = false;
    private ArrayDeque<StringBuilder> storage = new ArrayDeque<>();

    public CommandBase() {
        Collections.addAll(commandList, "help", "info", "show", "add", "update_id", "remove_by_id",
                "clear", "execute_script", "add_if_max", "remove_greater", "history", "min_by_furniture",
                "group_counting_by_transport", "print_field_descending_number_of_rooms", "show_my_obj");
        p = database.initializeFlat();
        date = new Date();
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        objectsOwners = database.initMap();
    }

    public void initializeCommandManager(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    void add() {
        addToHistory("add");
        try {
            writeLock.lock();
            StringBuilder ans = new StringBuilder();
            String login = users.get(commandManager.getCommandFromManager().getAddress());
            users.remove(commandManager.getCommandFromManager().getAddress());
            System.out.println(login + " = login пользователя");
            LOG.info("Поток, выполняющий команду: " + Thread.currentThread().getName());
            Command command = commandManager.getCommandFromManager();
            int id = database.writeFlat(command.getElement());
            p = database.getUpdateColl();
            command.getElement().setId(id);
            insertToAccessMap(command.getElement(), login);
            LOG.info("Элемент " + command.getElement().toString() + " успешно добавлен.");
            ans.append("Элемент успешно добавлен.").append("\n");
            pushAnswer(ans);
            writeLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            writeLock.unlock();
            errorReport();
            database.rollback();
        }
    }

    void addIfMax() {
        addToHistory("add_if_max");
        try {
            writeLock.lock();
            String login = users.get(commandManager.getCommandFromManager().getAddress());
            users.remove(commandManager.getCommandFromManager().getAddress());
            StringBuilder ans = new StringBuilder();
            Command command = commandManager.getCommandFromManager();
            Optional<Integer> max = p.stream().map(p -> p.getNumberOfRooms()).max(Integer::compareTo);
            if (max.isPresent()) {
                if (command.getElement().getNumberOfRooms() > max.get()) {
                    int id = database.writeFlat(command.getElement());
                    p = database.getUpdateColl();
                    database.insertIntoOwners(login, id);
                    objectsOwners = database.updateOwnersColl();
                    insertToAccessMap(command.getElement(), login);
                    ans.append("Элемент успешно добавлен.").append("\n");
                } else {
                    ans.append("Элеменет не можеть быть добавлен, т.к. элемент не максимальный.").append("\n");
                }
            } else {
                database.writeFlat(command.getElement());
                p = database.getUpdateColl();
                objectsOwners = database.updateOwnersColl();
                ans.append("Элемент успешно добавлен.").append("\n");
            }
            writeLock.unlock();
            pushAnswer(ans);
        } catch (SQLException e) {
            e.printStackTrace();
            errorReport();
            database.rollback();
        }
    }

    void clear() {
        addToHistory("clear");
        writeLock.lock();
        String login = users.get(commandManager.getCommandFromManager().getAddress());
        users.remove(commandManager.getCommandFromManager().getAddress());
        try {
            StringBuilder ans = new StringBuilder();
            if (objectsOwners.containsKey(login)) {
                ArrayList<Integer> al = objectsOwners.get(login);
                Iterator<Integer> iterator = al.iterator();
                System.out.println("Размер собранного листа: " + al.size());
                while (iterator.hasNext()) {
                    int id = iterator.next();
                    iterator.remove();
                    database.removeFlat(id);
                    p = database.getUpdateColl();
                    objectsOwners = database.updateOwnersColl();
                }
                ans.append("Все Ваши элементы успешно удалены.");
                writeLock.unlock();
            } else ans.append("У вас нет элементов, которые вы можете удалить.");
            pushAnswer(ans);
        } catch (SQLException e) {
            e.printStackTrace();
            errorReport();
            writeLock.unlock();
            database.rollback();
        }
    }

    private void advancedScript(String path, StringBuilder ans, String login) {
        //execute_script C:\IdeaProj\Lab5\JsonFile\Script2.txt
        try {
            if (paths.contains(path) || path.equals(previousFile)) {
                ans.append("Скрипт-файл не может быть выполнен, так как вызывает рекурсию").append("\n");
            } else {
                FileInputStream fileInputStream = new FileInputStream(path);
                Scanner scanner = new Scanner(fileInputStream);
                while (scanner.hasNext()) {
                    String[] args = scanner.nextLine().split(" ");
                    if (args[0].equals("execute_script")) {
                        paths.add(previousFile);
                        if (paths.contains(args[1]) || args[1].equals(path)) {
                            ans.append("Скрипт-файл не может быть выполнен, так как вызывает рекурсию").append("\n");
                            break;
                        } else {
                            paths.add(previousFile);
                            scriptRunner(args, login, ans);
                        }
                    } else {
                        scriptRunner(args, login, ans);
                    }
                }
                paths.clear();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            errorReport();
        }
    }

    void executeScript() {
        writeLock.lock();
        addToHistory("execute_script");
        try {
            storage.clear();
            StringBuilder answer = new StringBuilder();
            String login = users.get(commandManager.getCommandFromManager().getAddress());
            users.remove(commandManager.getCommandFromManager().getAddress());
            Command command = commandManager.getCommandFromManager();
            FileInputStream file = new FileInputStream(command.getFilename());
            Scanner input = new Scanner(file);
            while (input.hasNextLine()) {
                String[] args = input.nextLine().split(" ");
                if (args[0].equals("execute_script")) {
                    paths.add(command.getFilename());
                    if (args[1].equals(command.getFilename())) {
                        answer.append("Скрипт-файл не может быть выполнен, так как вызывает рекурсию.").append("\n");
                        break;
                    } else {
                        previousFile = command.getFilename();
                        answer.append(scriptRunner(args, login, new StringBuilder()));
                    }
                } else {
                    answer.append(scriptRunner(args, login, new StringBuilder()));
                }
            }
            System.out.println("answer = " + answer);
            System.out.println("answer end");
            pushAnswer(answer);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            pushAnswer(new StringBuilder().append("Произошла ошибка. Некорректный путь до файла."));
        } finally {
            writeLock.unlock();
        }
    }

    private StringBuilder scriptRunner(String[] args, String login, StringBuilder ans) {
        scriptFlag = true;
        switch (args[0]) {
            case "help": {
                help();
                ans.append(storage.pollFirst()).append("\n");
                break;
            }
            case "info": {
                info();
                ans.append(storage.pollFirst()).append("\n");
                System.out.println("ans = " + ans);
                break;
            }
            case "show_my_obj": {
                readLock.lock();
                try {
                    ans.append("Элементы, которые Вам принадлежат: ").append("\n");
                    LOG.info("Логин пользователя : " + login);
                    if (objectsOwners.containsKey(login)) {
                        ArrayList<Integer> al = objectsOwners.get(login);
                        if (al.size() != 0) {
                            for (Integer x : al) {
                                for (Flat s : p) {
                                    if (s.getId().equals(x)) {
                                        ans.append(s.toString()).append("\n");
                                    }
                                }
                            }
                        } else ans.append("У вас пока нет своих элементов.");
                    } else ans.append("У вас пока нет своих элементов.");
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
                finally {
                    readLock.unlock();
                }
            }
            case "show": {
                show();
                ans.append(storage.pollFirst()).append("\n");
                break;
            }
            case "add":
                writeLock.lock();
                if (args.length < 12)
                    ans.append("Элемент не может быть добавлен в коллекцию. Команда add не имеет достаточно аргументов.").append("\n");
                else {
                    try {
                        Flat flat = flatConstructor(args);
                        int id = database.writeFlat(flat);
                        database.insertIntoOwners(login, id);
                        flat.setId(id);
                        p = database.getUpdateColl();
                        objectsOwners = database.updateOwnersColl();
                        ans.append("Элемент успешно добавлен.").append("\n");
                    } catch (SQLException e) {
                        e.printStackTrace();
                        errorReport();
                        database.rollback();
                        writeLock.unlock();
                    }
                }
                writeLock.unlock();
                System.out.println("end of add");
                break;
            case "update_id":
                boolean flag = false;
                writeLock.lock();
                if (args.length < 13) {
                    ans.append("Команда update_id не может быть выполнена. Недостаточно аргументов.");
                } else {
                    int id = Integer.parseInt(args[1]);
                    for (Flat flat : p) {
                        if (flat.getId().equals(id)) {
                            if (scriptCheckAccess(login, id)) {
                                try {
                                    flat.setName(args[2]);
                                    flat.setCoordinates(new Coordinates(Integer.parseInt(args[3]), Double.parseDouble(args[4])));
                                    flat.setCreationDate(new Date());
                                    flat.setArea(Long.parseLong(args[5]));
                                    flat.setNumberOfRooms(Integer.parseInt(args[6]));
                                    flat.setFurniture(Boolean.parseBoolean(args[7]));
                                    flat.setView(View.valueOf(args[8]));
                                    flat.setTransport(Transport.valueOf(args[9]));
                                    flat.setHouse(new House(args[10], Integer.parseInt(args[11]), Long.parseLong(args[12])));
                                    database.removeFlat(id);
                                    int newId = database.writeFlat(flat);
                                    p = database.getUpdateColl();
                                    database.removeFromOwners(flat.getId());
                                    database.insertIntoOwners(login, newId);
                                    objectsOwners = database.updateOwnersColl();
                                    flag = true;
                                    System.out.println("метод успешно завершился");
                                    ans.append("Элемент успешно обновлён. Элементу изменён id, его новый id:  ").append(newId).append("\n");
                                }
                                catch (SQLException e){
                                    ans.append("Произошла ошибка при выполнении команды update_id.").append("\n");
                                }
                            } else {
                                flag = true;
                                ans.append("Элемент вам не принадлежит.");
                            }
                            break;
                        }
                    }
            }
                if (!flag)
                    ans.append("Такого элемента нет в коллекции.").append("\n");
                writeLock.unlock();
                System.out.println("end of update_id");
                break;
            case "remove_by_id":
                writeLock.lock();
                try {
                    boolean flag3 = false;
                    int id = Integer.parseInt(args[1]);
                    Iterator<Flat> flatIterator = p.iterator();
                    while (flatIterator.hasNext()) {
                        Flat element = flatIterator.next();
                        if (element.getId().equals(id)) {
                            if (scriptCheckAccess(login, id)) {
                                flatIterator.remove();
                                database.removeFlat(id);
                                ans.append("Элемент успешно удалён.").append("\n");
                                p = database.getUpdateColl();
                                database.removeFromOwners(id);
                                objectsOwners = database.updateOwnersColl();
                                flag3 = true;
                            } else {
                                ans.append("У Вас не прав модифицировать данный элемент.").append("\n");
                                flag3 = true;
                            }
                        }
                    }
                    if (!flag3) {
                        ans.append("Такого элемента нет в коллекции.").append("\n");
                        break;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    errorReport();
                    database.rollback();
                }
                finally {
                    writeLock.unlock();
                }
                break;

            case "clear": {
                writeLock.lock();
                try {
                    if (objectsOwners.containsKey(login)) {
                        for (Integer x : objectsOwners.get(login)) {
                            database.removeFlat(x);
                            p = database.getUpdateColl();
                            objectsOwners = database.updateOwnersColl();
                        }
                        ans.append("Все Ваши элементы успешно удалены.").append("\n");
                    } else {
                        ans.append("У вас нет элементов.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    database.rollback();
                    writeLock.unlock();
                }
                writeLock.unlock();
                break;
            }
            case "execute_script": {
                advancedScript(args[1], ans, login);
                break;
            }
            case "add_if_max": {

                if (args.length < 12) {
                    ans.append("Элемент не может быть добавлен в коллекцию. Команда add_if_max не имеет достаточно аргументов.").append("\n");
                } else {
                    addToHistory("add_if_max");
                    try {
                        writeLock.lock();
                        Optional<Integer> max = p.stream().map(p -> p.getNumberOfRooms()).max(Integer::compareTo);
                        Flat element = flatConstructor(args);
                        if (max.isPresent()) {
                            if (element.getNumberOfRooms() > max.get()) {
                                int id = database.writeFlat(element);
                                p = database.getUpdateColl();
                                objectsOwners = database.updateOwnersColl();
                                database.insertIntoOwners(login, id);
                                ans.append("Элемент успешно добавлен.").append("\n");
                            } else {
                                ans.append("Элеменет не можеть быть добавлен, т.к. элемент не максимальный.").append("\n");
                            }
                        } else {
                            int id = database.writeFlat(element);
                            database.insertIntoOwners(login, id);
                            objectsOwners = database.updateOwnersColl();
                            p = database.getUpdateColl();
                            ans.append("Элемент успешно добавлен.").append("\n");
                        }
                        writeLock.unlock();
                    } catch (SQLException e) {
                        e.printStackTrace();
                        errorReport();
                        database.rollback();
                    }
                    finally {
                        writeLock.unlock();
                    }

                }
                break;
            }
            case "remove_greater":
                writeLock.lock();
                try {
                    if (args.length < 12) {
                        ans.append("Команда remove_greater не может быть выполнена. Недостаточно аргументов.");
                    } else {
                        boolean flag0 = false;
                        Flat element = flatConstructor(args);
                        Iterator<Flat> iterator0 = p.iterator();
                        while (iterator0.hasNext()) {
                            Flat element0 = iterator0.next();
                            if (element0.getNumberOfRooms() - element.getNumberOfRooms() > 0) {
                                if (scriptCheckAccess(login, element0.getId())) {
                                    iterator0.remove();
                                    flag0 = true;
                                    database.removeFlat(element0.getId());
                                    p = database.getUpdateColl();
                                    ans.append("Элемент ").append(element0.getName()).append(" успешно удалён.").append("\n");
                                } else
                                    ans.append("У Вас не прав модифицировать данный элемент: ").append(element0.toString()).append("\n");
                            }
                        }
                        if (!flag0)
                            ans.append("Все Ваши элементы коллекции меньше заданного.").append("\n");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    database.rollback();

                }
                writeLock.unlock();
                break;

            case "history": {
                history();
                ans.append(storage.pollFirst()).append("\n");
                break;
            }
            case "min_by_furniture": {
                minByFurniture();
                ans.append(storage.pollFirst()).append("\n");
                break;
            }
            case "group_counting_by_transport": {
                groupCountingByTransport();
                ans.append(storage.pollFirst()).append("\n");
                break;
            }
            case "print_field_descending_number_of_rooms": {
                printFieldDescendingNumberOfRooms();
                ans.append(storage.pollFirst()).append("\n");
                break;
            }
            default:
                ans.append("Такой команды не существует").append("\n");
        }
        scriptFlag = false;
        return ans;
    }

    void groupCountingByTransport() {
        addToHistory("group_counting_by_transport");
        StringBuilder ans = new StringBuilder().append("Команда group_counting_by_transport:").append("\n");
        users.remove(commandManager.getCommandFromManager().getAddress());
        Map<Transport, Long> flatByTransport = p.stream().collect(Collectors.groupingBy(x -> x.getTransport(), Collectors.counting()));
        for (Map.Entry<Transport, Long> item : flatByTransport.entrySet()) {
            ans.append("Количество элементов с транспортом ").append(item.getKey()).append(": ").append(item.getValue()).append("\n");
        }
        ans.deleteCharAt(ans.length() - 1);
        if (!scriptFlag)
            pushAnswer(ans);
        else storage.addLast(ans);
    }

    void help() {
        readLock.lock();
        addToHistory("help");
        StringBuilder ans = new StringBuilder();
        users.remove(commandManager.getCommandFromManager().getAddress());
        ans.append("Список доступных команд: ").append("\n");
        for (String command : commandList) {
            ans.append(command).append("\n");
        }
        ans.deleteCharAt(ans.length() - 1);
        if (!scriptFlag)
            pushAnswer(ans);
        else storage.addLast(ans);
        readLock.unlock();
    }

    void history() {
        readLock.lock();
        addToHistory("history");
        StringBuilder ans = new StringBuilder();
        users.remove(commandManager.getCommandFromManager().getAddress());
        ans.append("Последние 13 использованных команд: ").append("\n");
        for (String s : commandQueue) {
            ans.append(s).append("\n");
        }
        ans.deleteCharAt(ans.length() - 1);
        if (!scriptFlag)
            pushAnswer(ans);
        else storage.addLast(ans);
        readLock.unlock();
    }

    void info() {
        addToHistory("info");
        users.remove(commandManager.getCommandFromManager().getAddress());
        Command command = commandManager.getCommandFromManager();
        StringBuilder ans = new StringBuilder();
        ans.append("Команда info: ").append("\n");
        ans.append("Класс коллекции: ").append(p.getClass()).append("\n");
        ans.append("Время создания коллекции: ").append(date).append("\n");
        ans.append("Количество элементов коллекции: ").append(p.size());
        if (!scriptFlag)
            pushAnswer(ans);
        else storage.addLast(ans);
    }

    void minByFurniture() {
        readLock.lock();
        addToHistory("min_by_furniture");
        users.remove(commandManager.getCommandFromManager().getAddress());
        StringBuilder ans = new StringBuilder().append("Элемент коллекции, c минимальным числом комнат: ").append("\n");
        if (p.size() != 0) {
            Flat element = p.stream().min(Comparator.comparingInt(Flat::getNumberOfRooms)).get();
            ans.append(element.getName()).append("; id: ").append(element.getId()).append("; Число комнат: ").append(element.getNumberOfRooms());
        } else {
            ans.append("Коллекция пуста.");
        }
        if (!scriptFlag)
            pushAnswer(ans);
        else storage.addLast(ans);
        readLock.unlock();
    }

    void printFieldDescendingNumberOfRooms() {
        addToHistory("print_field_descending_number_");
        readLock.lock();
        users.remove(commandManager.getCommandFromManager().getAddress());
        StringBuilder ans = new StringBuilder().append("Вывести значение поля number_of_rooms в порядке убывания:").append("\n");
        p.stream().map(x -> x.getNumberOfRooms())
                .sorted(Comparator.reverseOrder())
                .forEach(g -> ans.append(g).append("\n"));
        ans.deleteCharAt(ans.length() - 1);
        if (!scriptFlag)
            pushAnswer(ans);
        else storage.addLast(ans);
        readLock.unlock();
    }

    void removeById() {
        try {
            String login = users.get(commandManager.getCommandFromManager().getAddress());
            users.remove(commandManager.getCommandFromManager().getAddress());
            boolean flag = false;
            addToHistory("remove_by_id");
            writeLock.lock();
            StringBuilder ans = new StringBuilder();
            Command command = commandManager.getCommandFromManager();
            if (checkAccess(login, command)) {
                System.out.println("Проверка прав прошла успешно.");
                database.removeFlat(command.getId());
                ans.append("Элемент успешно удалён.");
                p.removeIf(x -> x.getId() == command.getId());
                objectsOwners = database.updateOwnersColl();
            } else {
                for (Flat flat : p) {
                    if (flat.getId() == command.getId()) {
                        ans.append("Элемент вам не принадлежит.");
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    ans.append("Такого элемента нет в коллекции. Для просмотра элементов введите команду show.");
                }
            }
            if (!scriptFlag)
                pushAnswer(ans);
            else storage.addLast(ans);
            writeLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            writeLock.unlock();
            errorReport();
            database.rollback();
        }
    }

    void showMyObj() {
        try {
            readLock.lock();
            StringBuilder ans = new StringBuilder();
            Command command = commandManager.getCommandFromManager();
            ans.append("Элементы, которые Вам принадлежат: ").append("\n");
            String login = users.get(command.getAddress());
            users.remove(command.getAddress());
            LOG.info("Логин пользователя : " + login);
            if (objectsOwners.containsKey(login)) {
                ArrayList<Integer> al = objectsOwners.get(login);
                System.out.println("al size = " + al.size());
                if (al.size() != 0) {
                    for (Integer x : al) {
                        for (Flat s : p) {
                            if (s.getId().equals(x)) {
                                ans.append(s.toString()).append("\n");
                            }
                        }
                    }
                } else ans.append("У вас пока нет своих элементов.");
            } else ans.append("У вас пока нет своих элементов.");
            pushAnswer(ans);
            readLock.unlock();
        } catch (NullPointerException e) {
            errorReport();
            e.printStackTrace();
            readLock.unlock();
        }
    }

    void removeGreater() {
        try {
            addToHistory("remove_greater");
            boolean flag = false;
            StringBuilder ans = new StringBuilder();
            writeLock.lock();
            Command command = commandManager.getCommandFromManager();
            String login = users.get(commandManager.getCommandFromManager().getAddress());
            users.remove(commandManager.getCommandFromManager().getAddress());
            if (objectsOwners.containsKey(login)) {
                ArrayList<Integer> col = objectsOwners.get(login);
                Iterator<Integer> iterator = col.iterator();
                while (iterator.hasNext()) {
                    int id = iterator.next();
                    Iterator<Flat> flatIterator = p.iterator();
                    while (flatIterator.hasNext()) {
                        Flat flat = flatIterator.next();
                        if (flat.getId().equals(id)) {
                            if (flat.getNumberOfRooms() - command.getElement().getNumberOfRooms() > 0) {
                                flag = true;
                                ans.append("Элемент ").append(flat.getName()).append(" успешно удалён.").append("\n");
                                iterator.remove();
                                flatIterator.remove();
                                database.removeFlat(flat.getId());
                                objectsOwners = database.updateOwnersColl();
                            }
                        }
                    }
                }
            } else {
                ans.append("У вас нет элементов.");
                flag = true;
            }
            if (!flag)
                ans.append("Все Ваши элементы меньше заданного.").append("\n");
            if (!scriptFlag)
                pushAnswer(ans);
            else storage.addLast(ans);
            writeLock.unlock();
        } catch (SQLException e) {
            e.printStackTrace();
            writeLock.unlock();
            errorReport();
            database.rollback();
        }
    }


    void show() {
        readLock.lock();
        users.remove(commandManager.getCommandFromManager().getAddress());
        addToHistory("show");
        StringBuilder ans = new StringBuilder().append("Команда show: ").append("\n");
        p.stream().sorted().forEachOrdered((x -> ans.append(x.toString()).append("\n")));
        ans.deleteCharAt(ans.length() - 1);
        if (!scriptFlag)
            pushAnswer(ans);
        else storage.addLast(ans);
        readLock.unlock();
    }

    void updateId() {
        writeLock.lock();
        String login = users.get(commandManager.getCommandFromManager().getAddress());
        users.remove(commandManager.getCommandFromManager().getAddress());
        System.out.println("login = " + login);
        StringBuilder ans = new StringBuilder();
        Command command = commandManager.getCommandFromManager();
        int id = command.getId();
        try {
            for (Flat flat : p) {
                if (flat.getId().equals(id)) {
                    if (checkAccess(login, command)) {
                        int newId = database.updateFlat(command);
                        p = database.getUpdateColl();
                        database.removeFromOwners(flat.getId());
                        database.insertIntoOwners(login, newId);
                        objectsOwners = database.updateOwnersColl();
                        ans.append("Элемент успешно обновлён. Элементу изменён id, его новый id:  ").append(newId);
                        break;
                    } else ans.append("Элемент вам не принадлежит.");
                }
            }
            pushAnswer(ans);
        } catch (SQLException e) {
            errorReport();
            database.rollback();
            writeLock.unlock();
        }
        writeLock.unlock();
    }

    ConcurrentLinkedQueue<StringBuilder> sendAnswer() {
        return answerQueue;
    }

    private void addToHistory(String command) {
        if (commandQueue.size() == 13) {
            commandQueue.removeLast();
            commandQueue.addFirst(command);
        } else commandQueue.addFirst(command);
    }

    private void pushAnswer(StringBuilder ans) {
        answerQueue.clear();
        answerQueue.add(ans);

    }

    void checkLogin() {
        try {
            StringBuilder ans = new StringBuilder();
            ans.append(database.checkLogin(commandManager.getCommandFromManager()));
            if (ans.toString().equals("Вы успешно зарегистрировались в системе.")) {
                insertNewUser(commandManager.getCommandFromManager().getLogin());
            }
            if (!(ans.toString().equals("Неверный логин или пароль.") || ans.toString().equals("Произошла ошибка, введите логин и пароль ещё раз."))) {
                //loginQueue.add(commandManager.getCommandFromManager().getLogin());
                users.put(commandManager.getCommandFromManager().getAddress(), commandManager.getCommandFromManager().getLogin());
                System.out.println("Логин зарегистрирован с адреса: " + commandManager.getCommandFromManager().getAddress());
            }
            pushAnswer(ans);
        } catch (SQLException e) {
            e.printStackTrace();
            errorReport();
        }
    }

    private boolean checkAccess(String login, Command command) {
        if (objectsOwners.containsKey(login)) {
            ArrayList<Integer> al = objectsOwners.get(login);
            for (int id : al) {
                if (id == command.getId()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void insertToAccessMap(Flat flat, String login) {
        if (objectsOwners.containsKey(login)) {
            objectsOwners.get(login).add(flat.getId());
            database.insertIntoOwners(login, flat.getId());
            LOG.info("Добавили элемент пользователю с логином: " + login);
            objectsOwners = database.updateOwnersColl();
        } else {
            objectsOwners.put(login, new ArrayList<>());
            objectsOwners.get(login).add(flat.getId());
            database.insertIntoOwners(login, flat.getId());
            objectsOwners = database.updateOwnersColl();
            LOG.info("Добавили элемент пользователю с логином: " + login);
        }
    }

    private void insertNewUser(String login) {
        objectsOwners.put(login, new ArrayList<Integer>());
    }

    private Flat flatConstructor(String[] args) {
        Flat element = new Flat();
        element.setName(args[1]);
        element.setCoordinates(new Coordinates(Integer.parseInt(args[2]), Double.parseDouble(args[3])));
        element.setCreationDate(new Date());
        element.setArea(Long.parseLong(args[4]));
        element.setNumberOfRooms(Integer.parseInt(args[5]));
        element.setFurniture(Boolean.parseBoolean(args[6]));
        element.setView(View.valueOf(args[7]));
        element.setTransport(Transport.valueOf(args[8]));
        element.setHouse(new House(args[9], Integer.parseInt(args[10]), Long.parseLong(args[11])));
        return element;
    }

    private boolean scriptCheckAccess(String login, Integer id) {
        if (objectsOwners.containsKey(login)) {
            for (Integer x : objectsOwners.get(login)) {
                if (x.equals(id))
                    return true;
            }
        }
        return false;
    }

    private void errorReport() {
        StringBuilder ans = new StringBuilder();
        ans.append("Произошла ошибка, попробуйте отправить команду ещё раз.");
        pushAnswer(ans);
    }
}
