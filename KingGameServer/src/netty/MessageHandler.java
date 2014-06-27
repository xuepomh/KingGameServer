package netty;

import java.io.IOException;
import java.net.SocketAddress;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;

import pk.PK;
import pk.PKManager;
import user.PKUser;
import user.UserManager;
import client.msg.received.SocketMessageReceived;
import client.msg.send.CrashLeavePKResultMessage2004;
import client.msg.send.HostLeavePKResultMessage2009;

public class MessageHandler extends SimpleChannelHandler {

	private static Logger logger = Logger.getLogger(MessageHandler.class);
	public static ChannelGroup channelGroup = new DefaultChannelGroup();
	@Override
	public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) {
		Channel channel = e.getChannel();
		SocketAddress ip = channel.getRemoteAddress();
		logger.info("Client connected in.." + ip);
		channelGroup.add(channel);
	}

	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Channel channel = (Channel) e.getChannel();
		ChannelBuffer channelBuffer = (ChannelBuffer) e.getMessage();
		short msgType = channelBuffer.readShort();
		SocketMessageReceived msg = SocketMessageFactory.getInstance()
				.getMessage(msgType);
		msg.handle(channelBuffer, channel);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) {
		logger.info("exceptionCaught");
		if (e.getCause() instanceof IOException) {
			
			dosomething(ctx, e);
		} else {
			e.getCause().printStackTrace();
		}
	}

	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) {
		logger.info("channelClosed");
		try {
			PKUser userTemp = UserManager.getInstance().removeUser(e.getChannel());
			if (userTemp != null) {
				long roomSqlID = userTemp.roomSqlID;
				if (roomSqlID != -1) {
					//房主退出
					if(PKManager.getInstance().getPKBySqlID(roomSqlID).channelHost==ctx.getChannel())
					{
						logger.info("房主channelClosed退出，解散房间,通知其他玩家退出房间，并且处理其他玩家退出逻辑");
						PKManager.getInstance().getPKBySqlID(roomSqlID).channelGroup
						.write(new HostLeavePKResultMessage2009(userTemp.id).pack());
						PKManager.getInstance().removePK(roomSqlID);
						
					}
					//非房主退出
					else
					{
						PK pk=PKManager.getInstance().getPKBySqlID(roomSqlID);
						PKUser user=pk.getPKUserByRoleName(userTemp.id);
						pk.channelGroup
						.write(new CrashLeavePKResultMessage2004(user.id,
								user.Camp, user.seatID,pk.sql_id).pack());
						PKManager.getInstance().getPKBySqlID(roomSqlID)
						.removePKUser(user.id, user.Camp, user.seatID);
						PKManager.getInstance().refreshPK();
					}
				
				}
			}
			logger.info("玩家关闭连接");
			logger.info("连接数量" + channelGroup.size());

		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	private void dosomething(ChannelHandlerContext ctx, ExceptionEvent e)
	{
		PKUser userTemp = UserManager.getInstance().removeUser(e.getChannel());
		if (userTemp != null) {
			long roomSqlID = userTemp.roomSqlID;
			if (roomSqlID != -1) {
				//房主退出
				if(PKManager.getInstance().getPKBySqlID(roomSqlID).channelHost==ctx.getChannel())
				{
					logger.info("房主exceptionCaught异常退出，解散房间,通知其他玩家退出房间，并且处理其他玩家退出逻辑");
					PKManager.getInstance().getPKBySqlID(roomSqlID).channelGroup
					.write(new HostLeavePKResultMessage2009(userTemp.id).pack());
					PKManager.getInstance().removePK(roomSqlID);
					
				}
				//非房主退出
				else
				{
					PK pk=PKManager.getInstance().getPKBySqlID(roomSqlID);
					PKUser user=pk.getPKUserByRoleName(userTemp.id);
					pk.channelGroup
					.write(new CrashLeavePKResultMessage2004(user.id,
							user.Camp, user.seatID,pk.sql_id).pack());
					PKManager.getInstance().getPKBySqlID(roomSqlID)
					.removePKUser(user.id, user.Camp, user.seatID);
					PKManager.getInstance().refreshPK();
				}
				
			
				logger.info("玩家断线，移除所在房间roomSqlID" + roomSqlID+"玩家数量"+UserManager.getInstance().getCurUserNum());
			}
		}
		UserManager.getInstance().removeUser(userTemp.id);
		logger.info("玩家断线");
	}
}
