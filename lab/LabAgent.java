package lab;

/**
 * TAC AgentWare
 * http://www.sics.se/tac        tac-dev@sics.se
 *
 * Copyright (c) 2001-2005 SICS AB. All rights reserved.
 *
 * SICS grants you the right to use, modify, and redistribute this
 * software for noncommercial purposes, on the conditions that you:
 * (1) retain the original headers, including the copyright notice and
 * this text, (2) clearly document the difference between any derived
 * software and the original, and (3) acknowledge your use of this
 * software in pertaining publications and reports.  SICS provides
 * this software "as is", without any warranty of any kind.  IN NO
 * EVENT SHALL SICS BE LIABLE FOR ANY DIRECT, SPECIAL OR INDIRECT,
 * PUNITIVE, INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES ARISING OUT
 * OF THE USE OF THE SOFTWARE.
 *
 * -----------------------------------------------------------------
 *
 * Author  : Joakim Eriksson, Niclas Finne, Sverker Janson
 * Created : 23 April, 2002
 * Updated : $Date: 2005/06/07 19:06:16 $
 *	     $Revision: 1.1 $
 * ---------------------------------------------------------
 * DummyAgent is a simplest possible agent for TAC. It uses
 * the TACAgent agent ware to interact with the TAC server.
 *
 * Important methods in TACAgent:
 *
 * Retrieving information about the current Game
 * ---------------------------------------------
 * int getGameID()
 *  - returns the id of current game or -1 if no game is currently plaing
 *
 * getServerTime()
 *  - returns the current server time in milliseconds
 *
 * getGameTime()
 *  - returns the time from start of game in milliseconds
 *
 * getGameTimeLeft()
 *  - returns the time left in the game in milliseconds
 *
 * getGameLength()
 *  - returns the game length in milliseconds
 *
 * int getAuctionNo()
 *  - returns the number of auctions in TAC
 *
 * int getClientPreference(int client, int type)
 *  - returns the clients preference for the specified type
 *   (types are TACAgent.{ARRIVAL, DEPARTURE, HOTEL_VALUE, E1, E2, E3}
 *
 * int getAuctionFor(int category, int type, int day)
 *  - returns the auction-id for the requested resource
 *   (categories are TACAgent.{CAT_FLIGHT, CAT_HOTEL, CAT_ENTERTAINMENT
 *    and types are TACAgent.TYPE_INFLIGHT, TACAgent.TYPE_OUTFLIGHT, etc)
 *
 * int getAuctionCategory(int auction)
 *  - returns the category for this auction (CAT_FLIGHT, CAT_HOTEL,
 *    CAT_ENTERTAINMENT)
 *
 * int getAuctionDay(int auction)
 *  - returns the day for this auction.
 *
 * int getAuctionType(int auction)
 *  - returns the type for this auction (TYPE_INFLIGHT, TYPE_OUTFLIGHT, etc).
 *
 * int getOwn(int auction)
 *  - returns the number of items that the agent own for this
 *    auction
 *
 * Submitting Bids
 * ---------------------------------------------
 * void submitBid(Bid)
 *  - submits a bid to the tac server
 *
 * void replaceBid(OldBid, Bid)
 *  - replaces the old bid (the current active bid) in the tac server
 *
 *   Bids have the following important methods:
 *    - create a bid with new Bid(AuctionID)
 *
 *   void addBidPoint(int quantity, float price)
 *    - adds a bid point in the bid
 *
 * Help methods for remembering what to buy for each auction:
 * ----------------------------------------------------------
 * int getAllocation(int auctionID)
 *   - returns the allocation set for this auction
 * void setAllocation(int auctionID, int quantity)
 *   - set the allocation for this auction
 *
 *
 * Callbacks from the TACAgent (caused via interaction with server)
 *
 * bidUpdated(Bid bid)
 *  - there are TACAgent have received an answer on a bid query/submission
 *   (new information about the bid is available)
 * bidRejected(Bid bid)
 *  - the bid has been rejected (reason is bid.getRejectReason())
 * bidError(Bid bid, int error)
 *  - the bid contained errors (error represent error status - commandStatus)
 *
 * quoteUpdated(Quote quote)
 *  - new information about the quotes on the auction (quote.getAuction())
 *    has arrived
 * quoteUpdated(int category)
 *  - new information about the quotes on all auctions for the auction
 *    category has arrived (quotes for a specific type of auctions are
 *    often requested at once).

 * auctionClosed(int auction)
 *  - the auction with id "auction" has closed
 *
 * transaction(Transaction transaction)
 *  - there has been a transaction
 *
 * gameStarted()
 *  - a TAC game has started, and all information about the
 *    game is available (preferences etc).
 *
 * gameStopped()
 *  - the current game has ended
 *
 */

import se.sics.tac.aw.*;
import se.sics.tac.util.ArgEnumerator;
import java.util.logging.*;

public class LabAgent extends AgentImpl {

	private static final Logger log = Logger.getLogger(LabAgent.class.getName());

	private static final boolean DEBUG = false;

	private float[] prices;
	private int[] premiumValues;

	protected void init(ArgEnumerator args) 
	{
		prices = new float[TACAgent.getAuctionNo()];
		this.premiumValues = new int[5];	//One for each day
	}

	public void quoteUpdated(Quote quote) 
	{
		int auction = quote.getAuction();
		int auctionCategory = TACAgent.getAuctionCategory(auction);

		float currentPrice=quote.getAskPrice();
		if(auctionCategory == TACAgent.CAT_FLIGHT)
		{  
			int alloc= agent.getAllocation(auction);
			int own = agent.getOwn(auction);
			if(alloc > 0 && alloc > own)
			{
				//get the seller bid update
				//check for the optimal pricey comparing currentPrice against previous price
				//if close to optimal price but the ticket.

				if(currentPrice > prices[auction])
				{
					Bid bid=new Bid(auction);
					bid.addBidPoint(alloc, currentPrice);
					agent.submitBid(bid);	}
			}
			else
			{
				prices[auction] = currentPrice;
			}
		}
		if (auctionCategory == TACAgent.CAT_HOTEL) 
		{
			int alloc = agent.getAllocation(auction);

			if (alloc > 0 && quote.hasHQW(agent.getBid(auction)) && quote.getHQW() < alloc) 
			{
				int day = TACAgent.getAuctionDay(auction);
				Bid bid = null;

				int cheapAuction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_CHEAP_HOTEL, day);
				int cheapPrice = (int) agent.getQuote(cheapAuction).getAskPrice();
				int expensiveAuction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_CHEAP_HOTEL, day);
				int expensivePrice = (int) agent.getQuote(expensiveAuction).getAskPrice();

				Quote cheapQuote = agent.getQuote(cheapAuction);
				Quote expensiveQuote = agent.getQuote(expensiveAuction);

				//Check whether it's worth bidding on the good hotel
				if(cheapPrice > expensivePrice - this.premiumValues[day])
				{
					if(agent.getQuote(expensiveAuction).isAuctionClosed() == false)
					{
						bid = new Bid(expensiveAuction);
						prices[expensiveAuction] = expensiveQuote.getAskPrice() + 1;
						bid.addBidPoint(alloc, prices[expensiveAuction]);							
					}
					else if(agent.getQuote(cheapAuction).isAuctionClosed() == false)
					{
						bid = new Bid(cheapAuction);
						prices[cheapAuction] = cheapQuote.getAskPrice() + 1;
						bid.addBidPoint(alloc, prices[cheapAuction]);									
					}

				}
				else
				{
					if(agent.getQuote(cheapAuction).isAuctionClosed() == false)
					{
						bid = new Bid(cheapAuction);
						prices[cheapAuction] = cheapQuote.getAskPrice() + 1;
						bid.addBidPoint(alloc, prices[cheapAuction]);									
					}
					else if(agent.getQuote(expensiveAuction).isAuctionClosed() == false)
					{
						bid = new Bid(expensiveAuction);
						prices[expensiveAuction] = expensiveQuote.getAskPrice() + 1;
						bid.addBidPoint(alloc, prices[expensiveAuction]);							
					}				
				}

				if (DEBUG) 
				{
					log.finest("submitting bid with alloc=" + agent.getAllocation(auction) + " own=" + agent.getOwn(auction));
				}

				if(bid != null)
				{
					agent.submitBid(bid);
				}
			}
		} 
		else if (auctionCategory == TACAgent.CAT_ENTERTAINMENT) 
		{
			Bid bid = new Bid(auction);
			//for (int t = 0; t < 8; t++)

			int alloc = agent.getAllocation(auction);
			int own = agent.getOwn(auction);
			//selling all the tickets we are allocated                  
			int rem=alloc-own;
			if(rem>0)
				prices[auction]=quote.getAskPrice()+10f;
			bid.addBidPoint(rem, prices[auction]);

			if (DEBUG) 
			{
				log.finest("submitting bid with alloc=" + agent.getAllocation(auction) + " own=" + agent.getOwn(auction));
			}

			agent.submitBid(bid);

		}
	}

	public void quoteUpdated(int auctionCategory) 
	{
		log.fine("All quotes for " + TACAgent.auctionCategoryToString(auctionCategory) + " has been updated");
	}

	public void bidUpdated(Bid bid) 
	{
		log.fine("Bid Updated: id=" + bid.getID() + " auction=" + bid.getAuction() + " state=" + bid.getProcessingStateAsString());
		log.fine("       Hash: " + bid.getBidHash());
	}

	public void bidRejected(Bid bid) 
	{
		log.warning("Bid Rejected: " + bid.getID());
		log.warning("      Reason: " + bid.getRejectReason() + " (" + bid.getRejectReasonAsString() + ')');
	}

	public void bidError(Bid bid, int status) {
		log.warning("Bid Error in auction " + bid.getAuction() + ": " + status
				+ " (" + agent.commandStatusToString(status) + ')');
	}

	public void gameStarted() {
		log.fine("Game " + agent.getGameID() + " started!");

		calculateAllocation();
		sendBids();
	}

	public void gameStopped() {
		log.fine("Game Stopped!");
	}

	public void auctionClosed(int auction) {
		log.fine("*** Auction " + auction + " closed!");
	}

	private void sendBids() {
		for (int i = 0, n = TACAgent.getAuctionNo(); i < n; i++) 
		{
			int alloc = agent.getAllocation(i) - agent.getOwn(i);
			float price = -1f;

			switch (TACAgent.getAuctionCategory(i)) 
			{
			case TACAgent.CAT_FLIGHT:
				if(alloc>0)
				{
					prices[i]=800;
				}
				break;
			case TACAgent.CAT_HOTEL:
				if (alloc > 0) 
				{					
					if(TACAgent.getAuctionType(i) == TACAgent.TYPE_GOOD_HOTEL)
					{
						//Calculate the price based on the premium value	
						int day = TACAgent.getAuctionDay(i);
						price = this.premiumValues[day];
					}
					else
					{
						//Set a base value
						price = 50;
					}

					this.prices[i] = price;
				}
				break;
			case TACAgent.CAT_ENTERTAINMENT:

				if (alloc < 0) 
				{
					// price=quote.getAskPrice()+10f;
					// prices [i]= price;

					price = 200;
					prices[i] = 200f;

				} 

				else if (alloc > 0) 
				{
					// price=agent.getBid(auction)-10f;
					//prices [i]= price;

					price = 50;
					prices[i] = 50f;
				}
				break;
			default:
				break;
			}

			if (price > 0) 
			{
				Bid bid = new Bid(i);
				bid.addBidPoint(alloc, price);

				if (DEBUG) 
				{
					log.finest("submitting bid with alloc=" + agent.getAllocation(i) + " own=" + agent.getOwn(i));
				}

				agent.submitBid(bid);
			}
		}
	}

	private void calculateAllocation() 
	{
		for (int i = 0; i < 8; i++) 
		{
			int inFlight = agent.getClientPreference(i, TACAgent.ARRIVAL);
			int outFlight = agent.getClientPreference(i, TACAgent.DEPARTURE);
			int hotel = agent.getClientPreference(i, TACAgent.HOTEL_VALUE);
			int type;

			// Get the flight preferences auction and remember that we are
			// going to buy tickets for these days. (inflight=1, outflight=0)
			int auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_INFLIGHT, inFlight);
			agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			auction = TACAgent.getAuctionFor(TACAgent.CAT_FLIGHT, TACAgent.TYPE_OUTFLIGHT, outFlight);
			agent.setAllocation(auction, agent.getAllocation(auction) + 1);

			// Try to get a good hotel unless the premium value is zero
			if (hotel > 0) 
			{
				type = TACAgent.TYPE_GOOD_HOTEL;
			} 
			else 
			{
				type = TACAgent.TYPE_CHEAP_HOTEL;
			}

			// allocate a hotel night for each day that the agent stays
			for (int d = inFlight; d < outFlight; d++) 
			{
				auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, type, d);
				log.finer("Adding hotel for day: " + d + " on " + auction);
				agent.setAllocation(auction, agent.getAllocation(auction) + 1);

				this.premiumValues[d] += hotel;
			}

			int eType = -1;

			while((eType = nextEntType(i, eType)) > 0) 
			{
				auction = bestEntDay(inFlight, outFlight, eType);
				log.finer("Adding entertainment " + eType + " on " + auction);
				agent.setAllocation(auction, agent.getAllocation(auction) + 1);
			}
		}

		//Calculate the average premium value
		for(int i = 0; i < 4; i++)
		{
			int auction = TACAgent.getAuctionFor(TACAgent.CAT_HOTEL, TACAgent.TYPE_GOOD_HOTEL, i);

			if(this.agent.getAllocation(auction) > 0)
			{
				this.premiumValues[i] /= this.agent.getAllocation(auction);
			}
		}
	}

	private int bestEntDay(int inFlight, int outFlight, int type) {
		for (int i = inFlight; i < outFlight; i++) 
		{
			int auction = TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, i);

			if (agent.getAllocation(auction) < agent.getOwn(auction)) 
			{
				return auction;
			}
		}

		// If no left, just take the first...
		return TACAgent.getAuctionFor(TACAgent.CAT_ENTERTAINMENT, type, inFlight);
	}

	private int nextEntType(int client, int lastType) {
		int e1 = agent.getClientPreference(client, TACAgent.E1);
		int e2 = agent.getClientPreference(client, TACAgent.E2);
		int e3 = agent.getClientPreference(client, TACAgent.E3);

		// At least buy what each agent wants the most!!!
		if ((e1 > e2) && (e1 > e3) && lastType == -1)
		{
			return TACAgent.TYPE_ALLIGATOR_WRESTLING;
		}
		if ((e2 > e1) && (e2 > e3) && lastType == -1)
		{
			return TACAgent.TYPE_AMUSEMENT;
		}
		if ((e3 > e1) && (e3 > e2) && lastType == -1)
		{
			return TACAgent.TYPE_MUSEUM;
		}

		return -1;
	}



	// -------------------------------------------------------------------
	// Only for backward compability
	// -------------------------------------------------------------------

	public static void main (String[] args) {
		TACAgent.main(args);
	}

} // DummyAgent