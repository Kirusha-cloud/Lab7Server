package Database;

import Flat.*;
import Lab7Client.Command;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.PriorityQueue;

public class Database {
    private static final Logger LOG = LogManager.getLogger();

    private Connection connection;
    private PriorityQueue<Flat> p;
    private HashMap<String, ArrayList<Integer>> s = new HashMap<>();
    private java.util.logging.Logger parentLog;
    private PrintWriter printWriter;

    public Database() {
        connect();
        p = initializeFlat();
        initDBLogger();
    }
    private void initDBLogger(){
        try {
            parentLog =  DriverManager.getDriver(DB_URL).getParentLogger();
            printWriter = new PrintWriter(System.out);
            DriverManager.setLogWriter(printWriter);
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
    private void connect() {
        try {
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
            connection.setAutoCommit(false);
            LOG.info("Соединение с базой данных установлено.");
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Integer writeFlat(Flat element) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("INSERT INTO flat VALUES (DEFAULT,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING flat_id ");
        ps.setString(1, element.getName());
        ps.setInt(2, element.getCoordinates().getX());
        ps.setDouble(3, element.getCoordinates().getY());
        ps.setDate(4, new java.sql.Date(new java.util.Date().getTime()));
        ps.setLong(5, element.getArea());
        ps.setInt(6, element.getNumberOfRooms());
        ps.setBoolean(7, element.isFurniture());
        ps.setString(8, element.getView().name());
        ps.setString(9, element.getTransport().name());
        ps.setString(10, element.getHouse().getName());
        ps.setInt(11, element.getHouse().getYear());
        ps.setLong(12, element.getHouse().getNumberOfLifts());
        ResultSet rs = ps.executeQuery();
        connection.commit();
        return updateCollection(rs, element);

    }

    public PriorityQueue<Flat> initializeFlat() {
        try {
            p = new PriorityQueue<>();
            ResultSet rs = connection.createStatement().executeQuery("SELECT * FROM flat");
            if (!rs.wasNull()) {
                while (rs.next()) {
                    p.add(constructFlat(rs));
                }
            }
            return p;
        } catch (SQLException e) {
            e.printStackTrace();
            return new PriorityQueue<Flat>();
        }
    }


    public void removeFlat(Integer id) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE FROM flat WHERE flat_id = ?");
        ps.setInt(1, id);
        ps.executeUpdate();
        p.removeIf(x -> x.getId().equals(id));
        connection.commit();
    }

    public int updateFlat(Command command) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("DELETE from flat WHERE flat_id = ?");
        p.removeIf(x -> x.getId() == command.getId());
        ps.setInt(1, command.getId());
        ps.executeUpdate();
        return writeFlat(command.getElement());
    }

    private Flat constructFlat(ResultSet rs) throws SQLException {
        Flat flat = new Flat();
        flat.setId(rs.getInt(1));
        flat.setName(rs.getString(2));
        flat.setCoordinates(new Coordinates(rs.getInt(3), rs.getDouble(4)));
        flat.setCreationDate(rs.getDate(5));
        flat.setArea(rs.getLong(6));
        flat.setNumberOfRooms(rs.getInt(7));
        flat.setFurniture(rs.getBoolean(8));
        flat.setView(View.valueOf(rs.getString(9)));
        flat.setTransport(Transport.valueOf(rs.getString(10)));
        flat.setHouse(new House(rs.getString(11), rs.getInt(12), rs.getLong(13)));
        return flat;
    }


    private Integer updateCollection(ResultSet rs, Flat flat) throws SQLException {
        rs.next();
        flat.setId(rs.getInt(1));
        p.add(flat);
        return flat.getId();
    }

    public String checkLogin(Command command) throws SQLException {
        PreparedStatement ps = connection.prepareStatement("SELECT  COUNT(*) FROM usertable WHERE loging = ?");
        ps.setString(1, command.getLogin());
        ResultSet rs = ps.executeQuery();
        rs.next();
        if (rs.getInt(1) != 0) {
            PreparedStatement prep = connection.prepareStatement("SELECT * FROM usertable WHERE loging = ?");
            prep.setString(1, command.getLogin());
            ResultSet set = prep.executeQuery();
            set.next();
            if (set.getString(3).equals(saltPass(new String(command.getHashbytes())))) {
                return "Вы успешно авторизованы.";
            } else {
                return "Неверный логин или пароль.";
            }
        } else return insertLogWithPas(command);

    }

    private String insertLogWithPas(Command command) throws SQLException {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT into usertable values (default, ?, ?);");
            ps.setString(1, command.getLogin());
            ps.setString(2, saltPass(new String(command.getHashbytes())));
            ps.executeUpdate();
            connection.commit();
            return "Вы успешно зарегистрировались в системе.";
        } catch (SQLException e) {
            e.printStackTrace();
            connection.rollback();
            return "Произошла ошибка, введите логин и пароль ещё раз.";
        }
    }

    public HashMap<String, ArrayList<Integer>> initMap() {
        try {
            HashMap<String, ArrayList<Integer>> map = new HashMap<String, ArrayList<Integer>>();
            PreparedStatement ps = connection.prepareStatement("SELECT * from owners");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String login = rs.getString(1);
                if (map.containsKey(login)) {
                    map.get(login).add(rs.getInt(2));
                } else {
                    map.put(login, new ArrayList<>());
                    map.get(login).add(rs.getInt(2));
                }
            }
            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public void insertIntoOwners(String login, int newId) {
        try {
            PreparedStatement ps = connection.prepareStatement("INSERT INTO owners VALUES (?, ?);");
            ps.setString(1, login);
            ps.setInt(2, newId);
            ps.executeUpdate();
            connection.commit();
            LOG.info("Добавлено в owners");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public HashMap<String, ArrayList<Integer>> updateOwnersColl() {
        s.clear();
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT * FROM owners");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String login = rs.getString(1);
                int id = rs.getInt(2);
                if (s.containsKey(login)) {
                    s.get(login).add(id);
                } else {
                    s.put(login, new ArrayList<>());
                    s.get(login).add(id);
                }
            }
            return s;
        } catch (SQLException e) {
            e.printStackTrace();
            return new HashMap<String, ArrayList<Integer>>();
        }

    }

    private String saltPass(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-384");
            PreparedStatement salt = connection.prepareStatement("SELECT * FROM salt");
            ResultSet saltSet = salt.executeQuery();
            saltSet.next();
            String saltString = saltSet.getString(1);
            return new String(md.digest((password + saltString).getBytes()));
        } catch (SQLException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }
    public void rollback()  {
        try {
            connection.rollback();
        } catch (SQLException e) {
            e.printStackTrace();        }
    }
    public PriorityQueue<Flat> getUpdateColl() {
        return p;
    }
    public void removeFromOwners(int id){
        try {

            PreparedStatement del = connection.prepareStatement("DELETE from owners where obj_id = ?");
            del.setInt(1, id);
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
}
