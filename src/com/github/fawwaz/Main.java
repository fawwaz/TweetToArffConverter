/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.github.fawwaz;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author Asus
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException{
        String test ="THE EXECUTIVE SUPER SALE up to 80% off Last 2 Days!THE EXECUTIVE boutiques @istanaplaza @InfoBdgDiskon @InfoBdgEvent http://t.co/tQyZRRjO26";
        String test2 ="Mau belajar bisnis, tp tak mau ribet ngurusin produksi & pengiriman? Mending hadir di #yukbisnisBandung @yubibdg http://t.co/duKdPKa24Z ajar";
        Converter converter = new Converter();
        converter.startConnection();
        //converter.parseTweet(test2);
        converter.doConvertion();
        converter.closeConnection();
    }
    
    static class Converter{
        HashSet<String> POSTag;
        Twokenize twokenizer;
        //ArrayList<String> tokens;
        Connection connection;
        PreparedStatement preparedstatement;
        ResultSet resultset;
        
        public Converter(){
            POSTag = new HashSet<>();
            twokenizer = new Twokenize();
        }
        
        public void doConvertion() throws IOException{
            // setup
            //String filename = "E:\\S2\\TA\\MyConverter\\test";
            String filename = "test";
            PrintWriter writer = new PrintWriter(new FileWriter(filename));
            // write header first.. 
            
            // Select from db.
            ArrayList<String> tweets = selectTweet();
            for (int i = 0; i < tweets.size(); i++) {
                String tobewriten = parseTweet(tweets.get(i));
                writer.write(tobewriten);
            }
            writer.close();
            // write to external file
        }
        
        private ArrayList<String> selectTweet(){
            ArrayList<String> tweets = new ArrayList<>();
            try{
                preparedstatement = connection.prepareStatement("SELECT * from filtered_tweet_final where label=1");
                resultset = preparedstatement.executeQuery();
                while(resultset.next()){
                    tweets.add(resultset.getString("tweet"));
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
            return tweets;
        }
        
        public String parseTweet(String tweet){
            ArrayList<String> tokens = new ArrayList<>();
            System.out.println("Parsing tweet "+tweet);
            List<String> tokenizeds = twokenizer.tokenizeRawTweetText(tweet);
            for (String tokenized : tokenizeds) {
                tokens.add(tokenized);
            }
            // Harus langsung di write agar token startnya bisa dapet..
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < tokens.size(); i++) {
                String token = tokens.get(i);
                sb.append("'"+token+"'");
                sb.append(",");
                sb.append("'"+getPreviousWord(i, tokens)+"'");
                sb.append(",");
                sb.append("'"+getNextWord(i, tokens)+"'");
                sb.append(",");
                sb.append("'"+getPOSTag(token)+"'");
                sb.append(",");
                sb.append("'"+getPreviousTag(i,tokens)+"'");
                sb.append(",");
                sb.append("'"+getNextTag(i,tokens)+"'");
                sb.append(",");
                sb.append("'"+getNumberFeature(token)+"'");
                sb.append(",");
                sb.append("'"+getPunctuationFeature(token)+"'");
                sb.append(",");
                sb.append("'"+getPlaceDirectiveFeature(token)+"'");
                sb.append(",");
                sb.append("'"+getURLFeature(token)+"'");
                sb.append(",");
                sb.append("'"+getMentionFeature(token)+"'");
                sb.append(",");
                sb.append("'"+getHashtagFeature(token)+"'");
                sb.append(",");
                sb.append("'"+getMonthFeature(token)+"'");
                sb.append(",");
                sb.append("'"+getGazeteer(token)+"'");
                sb.append("\n");
            }
            return sb.toString();
        }
        
        private String getNumberFeature(String token){
            if(token.matches("\\d+")){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getPunctuationFeature(String token){
            if(token.matches("\\p{P}")){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getPlaceDirectiveFeature(String token){
            if(token.matches("(di|@|d|ke|k)")){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getURLFeature(String token){
            if(token.matches("http://t\\.co/\\w+")){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getMentionFeature(String token){
            if(token.matches("@\\w+")){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getHashtagFeature(String token){
            if(token.matches("#\\w+")){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getMonthFeature(String token){
            if(token.matches(Twokenize.varian_bulan)){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getGazeteer(String token){
            if(isGazeteer(token)){
                return "YES";
            }else{
                return "NO";
            }
        }
        
        private String getDateSeparatorFeature(int i , ArrayList<String> tokens){
            if((i>0) &&(i<tokens.size()-1)){
                if(tokens.get(i-1).matches("\\d+") && tokens.get(i).matches("[/\\-]") && tokens.get(i+1).matches("\\d+")){
                    return "YES";
                }else{
                    return "NO";
                }
            }else{
                return "NO";
            }
        }
        
        
        private boolean isGazeteer(String gazetteer){
            try {
                preparedstatement = connection.prepareStatement("select location from gazetteer where location = ?");
                preparedstatement.setString(1, gazetteer);
                resultset = preparedstatement.executeQuery();
                if (resultset.next()) {
                    return true;
                } else {
                    return false;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return false;
        }
        
        public void startConnection(){
            try{
                Class.forName("com.mysql.jdbc.Driver");
            }catch(ClassNotFoundException e){
                e.printStackTrace();
            }
            String DB_USERNAME = "root";
            String DB_PASSWORD = "";
            String DB_URL = "mysql://localhost/";
            String DB_NAME = "mytomcatapp";
            String URL = "jdbc:" + DB_URL + DB_NAME;

            System.out.println("[INFO] Database Configuration variables");
            System.out.println("DB_USERNAME \t: "+ DB_USERNAME);
            System.out.println("DB_PASSWORD \t: "+ DB_PASSWORD);
            System.out.println("DB_URL	\t: "+ DB_URL);
            System.out.println("DB_NAME \t: "+ DB_NAME);
            System.out.println("URL \t\t:"+URL);
            
            System.out.println("[INFO] Connecting to DB");
            try {
                connection = (Connection) DriverManager.getConnection(URL, DB_USERNAME, DB_PASSWORD);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        
        public void closeConnection(){
            try {
                if (resultset != null) {
                    resultset.close();
                }
                if (preparedstatement != null) {
                    preparedstatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        public String getPOSTag(String token){
            try{
                preparedstatement = connection.prepareStatement("SELECT tipe_katadasar from tb_katadasar where katadasar = ?");
                preparedstatement.setString(1, token.toLowerCase());
                resultset = preparedstatement.executeQuery();
                if(resultset.next()){
                    return resultset.getString("tipe_katadasar");
                }else{
                    if (token.matches("\\bke\\w+an\\b|\\bpe\\w+an\\b|\\bpe\\w+\\b|\\b\\w+an\\b|\\bke\\w+\\b|\\b\\w+at\\b|\\b\\w+in\\b")) {
                        return "Nomina";
                    } else if (token.matches("\\bme\\w+\\b|\\bber\\w+\\b|\\b\\w+kan\\b|\\bdi\\w+\\b|\\bter\\w+\\b|\\b\\w+i\\b")) {
                        return "Verba";
                    } else if (token.matches("\\byuk\\b|\\bmari\\b|\\bayo\\b|\\beh\\b|\\bhai\\b")) {
                        return "Interjeksi";
                    } else {
                        return null;
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("[ERROR] connecting to database to retireve tipe katadasar (pos tag)");
            }
            return null;
        }
        
        private String getPreviousWord(int index,ArrayList<String> tokens){
            if(index == 0){
                return "<START>";
            }else{
                return tokens.get(index-1);
            }
        }
        
        private String getPreviousTag(int index,ArrayList<String> tokens){
            if(index == 0){
                return null;
            }else{
                return getPOSTag(tokens.get(index-1));
            }
        }
        
        private String getNextWord(int index,ArrayList<String> tokens){
            if(index == tokens.size() - 1){
                return "<END>";
            }else{
                return tokens.get(index+1);
            }
        }
        
        private String getNextTag(int index,ArrayList<String> tokens){
            if(index == tokens.size() - 1){
                return null;
            }else{
                return getPOSTag(tokens.get(index+1));
            }
        }
        
        

    }
    
}
