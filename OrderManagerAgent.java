





package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.List;


public class OrderManagerAgent extends Agent {

    private AID client;

    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("order-manager");
        sd.setName(getLocalName() + "-order-manager");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); } catch (FIPAException e) { e.printStackTrace(); }

        MessageTemplate t = MessageTemplate.or(
            MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
            MessageTemplate.MatchPerformative(ACLMessage.INFORM));

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = myAgent.receive(t);
                if (msg == null) { block(); return; }
                if (msg.getPerformative() == ACLMessage.REQUEST) handleOrder(msg);
                else relay(msg.getContent());
            }
        });
    }

    private void handleOrder(ACLMessage msg) {
        client = msg.getSender();
        String[] p = msg.getContent().split("\\|");
        String orderId = p[0], product = p[1], quartier = p[2], phone = p[3];
        double total = Catalog.productPrice(product) + Catalog.deliveryPrice(quartier);

        relay(orderId + "|INFO|" + product + "|" + quartier + "|" + phone + "|" + total);
        addBehaviour(new Negotiate(this, orderId));
    }

    private void relay(String content) {
        if (client == null) return;
        ACLMessage m = new ACLMessage(ACLMessage.INFORM);
        m.addReceiver(client);
        m.setContent(content);
        send(m);
    }

    /** Negociation Contract Net simplifiee avec les livreurs. */
    private class Negotiate extends Behaviour {
        private final String orderId, convId;
        private int step = 0;
        private long deadline;
        private int expected;
        private final List<AID> bidders = new ArrayList<>();
        private final List<Double> scores = new ArrayList<>();
        private MessageTemplate proposeTemplate;
        private boolean done = false;

        Negotiate(Agent a, String orderId) {
            super(a);
            this.orderId = orderId;
            this.convId = "deliv-" + orderId;
        }

        public void action() {
            if (step == 0) {
                List<AID> couriers = findCouriers();
                if (couriers.isEmpty()) {
                    relay(orderId + "|STATUS|Aucun livreur disponible|");
                    done = true;
                    return;
                }
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                for (AID c : couriers) cfp.addReceiver(c);
                cfp.setConversationId(convId);
                cfp.setContent(orderId);
                myAgent.send(cfp);

                expected = couriers.size();
                proposeTemplate = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(convId),
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
                deadline = System.currentTimeMillis() + 3000;
                step = 1;

            } else if (step == 1) {
                ACLMessage msg = myAgent.receive(proposeTemplate);
                if (msg != null) {
                    bidders.add(msg.getSender());
                    scores.add(Double.parseDouble(msg.getContent()));
                    if (bidders.size() >= expected) step = 2;
                } else if (System.currentTimeMillis() > deadline) {
                    step = 2;
                } else {
                    block(100);
                }

            } else {
                if (bidders.isEmpty()) {
                    relay(orderId + "|STATUS|Aucun livreur disponible|");
                    done = true;
                    return;
                }
                int best = 0;
                for (int i = 1; i < scores.size(); i++) if (scores.get(i) > scores.get(best)) best = i;
                AID winner = bidders.get(best);

                for (AID b : bidders) {
                    ACLMessage m = new ACLMessage(b.equals(winner) ? ACLMessage.ACCEPT_PROPOSAL : ACLMessage.REJECT_PROPOSAL);
                    m.addReceiver(b);
                    m.setConversationId(convId);
                    m.setContent(orderId);
                    myAgent.send(m);
                }
                relay(orderId + "|STATUS|Livreur assigne|" + winner.getLocalName());
                done = true;
            }
        }

        public boolean done() { return done; }

        private List<AID> findCouriers() {
            List<AID> result = new ArrayList<>();
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("courier");
            template.addServices(sd);
            try {
                for (DFAgentDescription d : DFService.search(myAgent, template)) result.add(d.getName());
            } catch (FIPAException e) { e.printStackTrace(); }
            return result;
        }
    }
}