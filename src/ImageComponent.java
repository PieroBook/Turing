import javax.accessibility.Accessible;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class ImageComponent extends Component implements SwingConstants, Accessible {
    private BufferedImage img;
    private String[] msg;
    private int numRighe = 1;
    private int reallen = 300;
    boolean own;

    ImageComponent(boolean owned,String text){
        this.own = owned;
        if(text.length() > 30){
            String[] arr = text.split("\\s");
            StringBuilder nuova = new StringBuilder();
            for(String s : arr){
                if((nuova.length() + s.length()) > 30*numRighe){
                    numRighe++;
                    nuova.append(" ").append("\n");
                }
                nuova.append(s).append(" ");
            }
            this.msg = nuova.toString().split("\\n");
        }else{
            this.msg = new String[1];
            this.msg[0] = text;
        }
        if(numRighe == 1){
            reallen = this.msg[0].length()*10;
        }
        setSize(300,33*(numRighe+1));
        setMinimumSize(this.getSize());
        setMaximumSize(this.getSize());
        setPreferredSize(this.getSize());
        try {
            if (owned)
                img = ImageIO.read(new File("drawable/msg_mine.png"));
            else
                img = ImageIO.read(new File("drawable/msg_other.png"));
        }catch (Exception e){
            System.err.println("Fallimento creazione fumetto grafica!!");
        }
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        int x = 0;
        if(own)
            x = 300-reallen;
        g.drawImage(img,x,0,reallen,27*(numRighe+1),this);
        x += 13;
        if(!own)
            x += 12;
        for(int i = 0; i<numRighe;i++)
            g.drawString(msg[i],x,25*(i+1));
    }
}