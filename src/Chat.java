import java.awt.*;

class Chat extends Panel {
    private ScrollPane vista;
    private Panel pannello;
    private int numMsgs = 0;

    Chat(){
        super();
        setLayout(new GridLayout(0,1));
        pannello = new Panel(new GridBagLayout());
        vista = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
        vista.add(pannello);
        this.add(vista);
        this.setSize(330,375);
    }

    void addMsg(boolean owned,String text){
        // Crea il messaggio grafico
        ImageComponent fumetto = new ImageComponent(owned,text);
        // Imposto vincoli di posizionamento messaggio
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.VERTICAL;
        c.gridx = 0;
        c.gridy = numMsgs++;
        // Aggancia messaggio al frame di visualizzazione
        pannello.add(fumetto,c);
        // Richiede validazione grafica e scrollDown
        validate();
        vista.setScrollPosition(0,vista.getHAdjustable().getMaximum()+vista.getHScrollbarHeight());
        validate();
    }
}