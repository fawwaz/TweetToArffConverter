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
import java.util.Iterator;
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
        Twokenize twokenizer;
        //ArrayList<String> tokens;
        Connection connection;
        PreparedStatement preparedstatement;
        ResultSet resultset;
        
        // For storing attribute value
        HashSet<String> words;
        HashSet<String> postags;
        HashSet<String> boolval;
        HashSet<String> labels;
        
        public static final String start_token = "<START>";
        public static final String end_token = "<END>";
        
        public PrintWriter writer;
        
        public Converter(){
            twokenizer = new Twokenize();
            words = new HashSet<>();
            postags = new HashSet<>();
            boolval = new HashSet<>();
            labels = new HashSet<>();
            
            words.add(start_token);
            words.add(end_token);
            
            boolval.add("YES");
            boolval.add("NO");
            
        }
        
        public void doConvertion() throws IOException{
            // setup
            //String filename = "E:\\S2\\TA\\MyConverter\\test";
            String filename = "test";
            writer = new PrintWriter(new FileWriter(filename));
            writer.write("\"token\",\"prev_word\",\"next_word\",\"tag\",\"prev_tag\",\"next_tag\",\"is_number\",\"is_punctuation\",\"is_place_directive\",\"is_url\",\"is_twitter_account\",\"is_hashtag\",\"is_month_name\",\"is_gazeteer\",\"label\"");
            // write header first.. 
            
            // Select from db.
//            ArrayList<String> tweets = selectTweet();
//            for (int i = 0; i < tweets.size(); i++) {
//                String tobewriten = parseTweet(tweets.get(i));
//                writer.write(tobewriten);
//            }
            // put arff header in bottom but next to be moved to top..
            writer.write(parseTweet());
            //writer.write(getArffHeader());
            
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
        
        private ArrayList<Pair> selectTokenized(){
            ArrayList<Pair> result = new ArrayList<>();
            try{
                preparedstatement = connection.prepareStatement("SELECT token,label2,twitter_tweet_id from anotasi_tweet_final");
                resultset = preparedstatement.executeQuery();
                while(resultset.next()){
                    Pair p = new Pair();
                    if(resultset.getString("token").equals("'")){
                        p.token = "_PETIK_";
                    }else if(resultset.getString("token").equals("\"")){
                        p.token = "_PETIK_GANDA_";
                    }else{
                        p.token = resultset.getString("token");
                    }
                    
                    p.label = resultset.getString("label2");
                    labels.add(p.label);
                    words.add(p.token);
                    p.tweet_id = resultset.getLong("twitter_tweet_id");
                    result.add(p);
                }
            }catch(SQLException e){
                e.printStackTrace();
            }
            
            for (int i = 0; i < result.size(); i++) {
                if(i==0){
                    result.get(i).isfirst = true;
                }else if(i==result.size()-1){
                    result.get(i).islast = true;
                }else{
                    if(!result.get(i).tweet_id.equals(result.get(i+1).tweet_id)){
                        result.get(i).islast = true;
                    }
                    if(!result.get(i).tweet_id.equals(result.get(i-1).tweet_id)){
                        result.get(i).isfirst = true;
                    }
                }
            }
            
            return result;
        }
        
        private String getArffHeader(){
            String _words = getStringSet(words);
            String _postags = getStringSet(postags);
            String _boolval = getStringSet(boolval);
            String _labels = getStringSet(labels);
            StringBuffer sb = new StringBuffer();
            sb.append("@relation Tweets\n");
            sb.append("\n");
            sb.append("@attribute current_word "+_words+ "\n");
            sb.append("@attribute prev_word " +_words+ "\n" );
            sb.append("@attribute next_word " +_words+ "\n");
            sb.append("@attribute pos_tag "+_postags+ "\n" );
            sb.append("@attribute prev_pos_tag "+_postags+ "\n");
            sb.append("@attribute next_pos_tag "+_postags+ "\n");
            sb.append("@attribute is_number " +_boolval + "\n" );
            sb.append("@attribute is_punctuation " +_boolval + "\n");
            sb.append("@attribute is_place_directive " +_boolval + "\n");
            sb.append("@attribute is_url " +_boolval + "\n");
            sb.append("@attribute is_twitter_account " +_boolval + "\n");
            sb.append("@attribute is_hashtag " +_boolval + "\n");
            sb.append("@attribute is_month_name " +_boolval + "\n");
            sb.append("@attribute is_gazeteer " +_boolval + "\n");
            sb.append("@attribute label " +_labels + "\n");
            sb.append("\n");
            sb.append("@data");
            return sb.toString();
        }
        
        public String parseTweet(){
            ArrayList<Pair> tokenizeds = selectTokenized();
            // Harus langsung di write agar token startnya bisa dapet..
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < tokenizeds.size(); i++) {
                //'token','prev_word','next_word','tag','prev_tag','next_tag','is_number','is_punctuation','is_place_directive','is_url','is_twitter_account','is_hashtag','is_month_name','is_gazeteer','label'
                String token = tokenizeds.get(i).token;
                sb.append("\""+token+"\"");
                sb.append(",");
                sb.append("\""+getPreviousWord(i, tokenizeds)+"\"");
                sb.append(",");
                sb.append("\""+getNextWord(i, tokenizeds)+"\"");
                sb.append(",");
                sb.append("\""+getPOSTag(token)+"\"");
                sb.append(",");
                sb.append("\""+getPreviousTag(i, tokenizeds)+"\"");
                sb.append(",");
                sb.append("\""+getNextTag(i,tokenizeds)+"\"");
                sb.append(",");
                sb.append("\""+getNumberFeature(token)+"\"");
                sb.append(",");
                sb.append("\""+getPunctuationFeature(token)+"\"");
                sb.append(",");
                sb.append("\""+getPlaceDirectiveFeature(token)+"\"");
                sb.append(",");
                sb.append("\""+getURLFeature(token)+"\"");
                sb.append(",");
                sb.append("\""+getMentionFeature(token)+"\"");
                sb.append(",");
                sb.append("\""+getHashtagFeature(token)+"\"");
                sb.append(",");
                sb.append("\""+getMonthFeature(token)+"\"");
                sb.append(",");
                sb.append("\""+getGazeteer(token)+"\"");
                sb.append(",");
                sb.append("\""+tokenizeds.get(i).label+"\"");
                /**/
                sb.append("\n");
                
                if(i%100==0){
                    System.out.println("i :"+i);
                    writer.write(sb.toString());
                    sb = new StringBuffer();
                }
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
                    postags.add(resultset.getString("tipe_katadasar"));
                    return resultset.getString("tipe_katadasar");
                }else{
                    if (token.matches("\\bke\\w+an\\b|\\bpe\\w+an\\b|\\bpe\\w+\\b|\\b\\w+an\\b|\\bke\\w+\\b|\\b\\w+at\\b|\\b\\w+in\\b")) {
                        postags.add("Nomina");
                        return "Nomina";
                    } else if (token.matches("\\bme\\w+\\b|\\bber\\w+\\b|\\b\\w+kan\\b|\\bdi\\w+\\b|\\bter\\w+\\b|\\b\\w+i\\b")) {
                        postags.add("Verba");
                        return "Verba";
                    } else if (token.matches("\\byuk\\b|\\bmari\\b|\\bayo\\b|\\beh\\b|\\bhai\\b")) {
                        postags.add("Interjeksi");
                        return "Interjeksi";
                    } else {
                        postags.add("_NULL_");
                        return "_NULL_";
                    }
                }
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("[ERROR] connecting to database to retireve tipe katadasar (pos tag)");
            }
            return "_NULL_";
        }
        
        private String getPreviousWord(int index,ArrayList<Pair> tokens){
            if(tokens.get(index).isfirst){
                return start_token;
            }else{
                return tokens.get(index-1).token;
            }
        }
        
        private String getPreviousTag(int index,ArrayList<Pair> tokens){
            if (tokens.get(index).isfirst) {
                return "_NULL_";
            } else {
                return getPOSTag(tokens.get(index - 1).token);
            }
        }
        
        private String getNextWord(int index,ArrayList<Pair> tokens){
            if(tokens.get(index).islast){
                return end_token;
            }else{
                return tokens.get(index+1).token;
            }
        }
        
        private String getNextTag(int index,ArrayList<Pair> tokens){
            if(tokens.get(index).islast){
                return "_NULL_";
            }else{
                return getPOSTag(tokens.get(index+1).token);
            }
        }
        
        private String getStringSet(HashSet<String> set){
            StringBuffer sb = new StringBuffer();
            sb.append("{");
            Iterator iterator = set.iterator();
            while(iterator.hasNext()){
                sb.append("'"+iterator.next()+"',");
            }
            sb.deleteCharAt(sb.length()-1); // remove last ,
            sb.append("}");
            return sb.toString();
        }
        
        public class Pair{
            public Long tweet_id;
            public String token;
            public String label;
            public boolean isfirst;
            public boolean islast;
        }
        
    }
    
}
