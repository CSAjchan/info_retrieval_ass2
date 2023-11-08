public class customQuery {
    public int Id;
    public String Text;

    public customQuery(int id, String text){
        Id = id;
        Text = text.replaceFirst(".W", "");
    }
}
