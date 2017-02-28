import input.Order;
import input.User;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Created by Sharmistha on 2/28/2017.
 */
public class Command implements Callable<String> {
    String cmd;

    public Command(String s){
        this.cmd = s;
    }

    @Override
    public String call() throws Exception {
        return this.processCommand(this.cmd);
    }


    public synchronized String processCommand(String req) throws IOException, InterruptedException {
        String[] tokens = req.split(" ");
        String result = "result";
        switch(tokens[0]){
            case "setmode":
                Server.curMode = (tokens[1]);
                result = "changed server mode to " +Server.curMode;
                break;
            case "purchase":
                String name = tokens[1];
                String product = tokens[2];
                Integer quantity = new Integer(tokens[3]);

                result = tryPurchase(name, product, quantity);
                break;
            case "cancel":
                Integer id = new Integer(tokens[1]);
                result = cancelOrder(id);
                break;

            case "search":
                String UserName = tokens[1];
                result = findUserOrders(UserName);
                break;

            case "list":
                result = listAllProducts();
                break;
        }
        return result;
    }


    public synchronized String listAllProducts() {
        String result = "";
        Iterator it = Server.Inventory.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            result = result + pair.getKey() + " " + pair.getValue() + "\n";
        }

        return result;
    }

    public synchronized String findUserOrders(String userName) {
        String result = "";
        User user = new User(userName);
        User foundUser;
        int userIndex;
        if(Server.users.contains(user)){
            userIndex = Server.users.indexOf(user);
            foundUser = Server.users.get(userIndex);
            for(int i=0; i<foundUser.orders.size(); i++){
                Order order = foundUser.orders.get(i);
                Integer orderID = order.getOrderID();
                String productName = order.getProduct();
                Integer quantity = order.getQuantity();
                result = result+orderID+" "+productName+" "+quantity+"\n";
            }
        }
        else{
            result = "No order found for "+userName;
        }
        return result;

    }

    public synchronized String cancelOrder(Integer id) {
        String result = "";
        for (int i = 0; i < Server.allOrders.size() ; i++) {
            if(Server.allOrders.get(i).getOrderID() == id){
                //TODO: lock?
                String name = Server.allOrders.get(i).getName();
                String product = Server.allOrders.get(i).getProduct();
                int quantity = Server.allOrders.get(i).getQuantity();
                //TODO: Lock
                User user = getUser(name);
                user.removeOrder(id);
                Server.Inventory.put(product, Server.Inventory.get(product)+quantity);
                Server.allOrders.remove(i);
                result = "Order "+id+" is canceled";
                return result;
            }
        }

        result = id+" not found, no such order";
        return result;
    }

    public synchronized String tryPurchase(String name, String product, Integer quantity) {
        String result = "";
        Integer qtyAvail = Server.Inventory.get(product);
        if(qtyAvail == null){
            result = "Not Available - We do not sell this product";
        }
        else if(qtyAvail < quantity){
            result = "Not Available - Not enough items";
        }
        else{
            //TODO: lock
            int id = Server.orderID;
            Server.orderID++;
            User user = getUser(name);
            user.createOrder(id, product, quantity);
            Order newOrder = new Order(name, id, product, quantity);
            Server.users.add(user);
            Server.allOrders.add(newOrder);
            Server.Inventory.put(product, Server.Inventory.get(product)-quantity);

            result = "Your order has been placed, " +id+" "+name+" "+product+" "+quantity;
        }

        return result;
    }

    public synchronized User getUser(String name) {
        User user = new User(name);
        int userIndex;
        if(Server.users.contains(user)){
            userIndex = Server.users.indexOf(user);
            user = Server.users.get(userIndex);
        }
        return user;
    }


}
