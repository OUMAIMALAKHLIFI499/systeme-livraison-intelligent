



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

import java.util.Random;


public class CourierAgent extends Agent {

    private final Random random = new Random();
    private int busy = 0;

    protected void setup() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("courier");
        sd.setName(getLocalName() + "-courier");
        dfd.addServices(sd);
        try { DFService.register(this, dfd); } catch (FIPAException e) { e.printStackTrace(); }

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) { block(); return; }

                if (msg.getPerformative() == ACLMessage.CFP) {
                    double score = Math.max(0.05, 0.5 + random.nextDouble() * 0.5 - busy * 0.2);
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(String.valueOf(score));
                    send(reply);

                } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    busy++;
                    addBehaviour(new Deliver(myAgent, msg.getContent(), msg.getSender()));
                }
            }
        });
    }

    private class Deliver extends Behaviour {
        private final String orderId;
        private final AID manager;
        private final String[] steps = {"Commande recuperee", "En route", "Livree"};
        private final long[] delaysBeforeNext = {9000, 10000};   
        private int index = 0;
        private long next;
        private boolean done = false;

        Deliver(Agent a, String orderId, AID manager) {
            super(a);
            this.orderId = orderId;
            this.manager = manager;
            this.next = System.currentTimeMillis() + 2000;
        }

        public void action() {
            long now = System.currentTimeMillis();
            if (now < next) { block(next - now); return; }

            ACLMessage m = new ACLMessage(ACLMessage.INFORM);
            m.addReceiver(manager);
            m.setContent(orderId + "|STATUS|" + steps[index] + "|" + getLocalName());
            send(m);


            if (index < delaysBeforeNext.length) {
                next = System.currentTimeMillis() + delaysBeforeNext[index];
            }
            index++;
            if (index >= steps.length) { busy = Math.max(0, busy - 1); done = true; }
        }

        public boolean done() { return done; }
    }
}