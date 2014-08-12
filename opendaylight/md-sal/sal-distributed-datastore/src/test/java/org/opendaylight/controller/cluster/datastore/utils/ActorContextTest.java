package org.opendaylight.controller.cluster.datastore.utils;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import akka.testkit.JavaTestKit;
import org.junit.Test;
import org.opendaylight.controller.cluster.datastore.AbstractActorTest;
import org.opendaylight.controller.cluster.datastore.ClusterWrapper;
import org.opendaylight.controller.cluster.datastore.Configuration;
import org.opendaylight.controller.cluster.datastore.messages.FindLocalShard;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardFound;
import org.opendaylight.controller.cluster.datastore.messages.LocalShardNotFound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class ActorContextTest extends AbstractActorTest{
    @Test
    public void testResolvePathForRemoteActor(){
        ActorContext actorContext =
            new ActorContext(mock(ActorSystem.class), mock(ActorRef.class),mock(
                ClusterWrapper.class),
                mock(Configuration.class));

        String actual = actorContext.resolvePath(
            "akka.tcp://system@127.0.0.1:2550/user/shardmanager/shard",
            "akka://system/user/shardmanager/shard/transaction");

        String expected = "akka.tcp://system@127.0.0.1:2550/user/shardmanager/shard/transaction";

        assertEquals(expected, actual);
    }

    @Test
    public void testResolvePathForLocalActor(){
        ActorContext actorContext =
            new ActorContext(getSystem(), mock(ActorRef.class), mock(ClusterWrapper.class),
                mock(Configuration.class));

        String actual = actorContext.resolvePath(
            "akka://system/user/shardmanager/shard",
            "akka://system/user/shardmanager/shard/transaction");

        String expected = "akka://system/user/shardmanager/shard/transaction";

        assertEquals(expected, actual);

        System.out.println(actorContext
            .actorFor("akka://system/user/shardmanager/shard/transaction"));
    }


    private static class MockShardManager extends UntypedActor {

        private final boolean found;
        private final ActorRef actorRef;

        private MockShardManager(boolean found, ActorRef actorRef){

            this.found = found;
            this.actorRef = actorRef;
        }

        @Override public void onReceive(Object message) throws Exception {
            if(found){
                getSender().tell(new LocalShardFound(actorRef), getSelf());
            } else {
                getSender().tell(new LocalShardNotFound(((FindLocalShard) message).getShardName()), getSelf());
            }
        }

        private static Props props(final boolean found, final ActorRef actorRef){
            return Props.create(new Creator<MockShardManager>() {

                @Override public MockShardManager create()
                    throws Exception {
                    return new MockShardManager(found,
                        actorRef);
                }
            });
        }
    }

    @Test
    public void testExecuteLocalShardOperationWithShardFound(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(true, shardActorRef));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    Object out = actorContext.executeLocalShardOperation("default", "hello", duration("1 seconds"));

                    assertEquals("hello", out);


                    expectNoMsg();
                }
            };
        }};

    }

    @Test
    public void testExecuteLocalShardOperationWithShardNotFound(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(false, null));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    Object out = actorContext.executeLocalShardOperation("default", "hello", duration("1 seconds"));

                    assertNull(out);


                    expectNoMsg();
                }
            };
        }};

    }


    @Test
    public void testFindLocalShardWithShardFound(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef shardActorRef = getSystem().actorOf(Props.create(EchoActor.class));

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(true, shardActorRef));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    Object out = actorContext.findLocalShard("default");

                    assertEquals(shardActorRef, out);


                    expectNoMsg();
                }
            };
        }};

    }

    @Test
    public void testFindLocalShardWithShardNotFound(){
        new JavaTestKit(getSystem()) {{

            new Within(duration("1 seconds")) {
                protected void run() {

                    ActorRef shardManagerActorRef = getSystem()
                        .actorOf(MockShardManager.props(false, null));

                    ActorContext actorContext =
                        new ActorContext(getSystem(), shardManagerActorRef , mock(ClusterWrapper.class),
                            mock(Configuration.class));

                    Object out = actorContext.findLocalShard("default");

                    assertNull(out);


                    expectNoMsg();
                }
            };
        }};

    }
}