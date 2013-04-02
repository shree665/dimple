/*******************************************************************************
*   Copyright 2012 Analog Devices, Inc.
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************/

package com.analog.lyric.dimple.solvers.particleBP;

import java.util.Arrays;

import com.analog.lyric.dimple.FactorFunctions.core.FactorFunction;
import com.analog.lyric.dimple.model.DimpleException;
import com.analog.lyric.dimple.model.Factor;
import com.analog.lyric.dimple.model.INode;
import com.analog.lyric.dimple.model.RealDomain;
import com.analog.lyric.dimple.model.VariableBase;
import com.analog.lyric.dimple.solvers.core.SRealVariableBase;
import com.analog.lyric.dimple.solvers.interfaces.ISolverFactor;
import com.analog.lyric.dimple.solvers.interfaces.ISolverNode;

public class SRealVariable extends SRealVariableBase
{
	protected Double[] _particleValues;
	protected int _numParticles = 1;
	protected int _resamplingUpdatesPerSample = 1;
	protected double _resamplingProposalStdDev = 1;
	protected double _initialParticleMin = 0;
	protected double _initialParticleMax = 0;
	protected boolean _initialParticleRangeSet = false;
	protected FactorFunction _input;
	protected RealDomain _domain;
	double[][] _inPortMsgs = new double[0][];
	double[][] _logInPortMsgs = new double[0][];
	ParticleBPSolverVariableToFactorMessage[] _outMsgArray = new ParticleBPSolverVariableToFactorMessage[0];
	double [] _logWeight;
	protected double _beta = 1;



	public SRealVariable(VariableBase var)  
	{
		super(var);

		if (!(var.getDomain() instanceof RealDomain))
			throw new DimpleException("Expected real domain");

		_domain = (RealDomain)var.getDomain();

	}

	public void updateEdge(int outPortNum)
	{
		final double minLog = -100;
		int M = _numParticles;
		int D = _var.getSiblings().size();
		double maxLog = Double.NEGATIVE_INFINITY;


		double[] outMsgs = _outMsgArray[outPortNum].messageValues;

		for (int m = 0; m < M; m++)
		{
			double prior = 1;
			if (_input != null)
				try {prior = _input.eval(new Object[]{_particleValues[m]});} catch (Exception e) {e.printStackTrace(); System.exit(1);}
				double out = (prior == 0) ? minLog : Math.log(prior) * _beta;

				for (int d = 0; d < D; d++)
				{
					if (d != outPortNum)		// For all ports except the output port
					{
						double tmp = _inPortMsgs[d][m];
						out += (tmp == 0) ? minLog : Math.log(tmp);
					}
				}

				// Subtract the log weight
				out -= _logWeight[m];

				if (out > maxLog) maxLog = out;
				outMsgs[m] = out;
		}

		//create sum
		double sum = 0;
		for (int m = 0; m < M; m++)
		{
			double out = Math.exp(outMsgs[m] - maxLog);
			outMsgs[m] = out;
			sum += out;
		}

		//calculate message by dividing by sum
		for (int m = 0; m < M; m++)
			outMsgs[m] /= sum;

	}

	public void update()
	{
		final double minLog = -100;
		int M = _numParticles;
		int D = _var.getSiblings().size();


		//Compute alphas
		double[] alphas = new double[M];
		for (int m = 0; m < M; m++)
		{
			double prior = 1;
			if (_input != null)
				try {prior = _input.eval(new Object[]{_particleValues[m]});} catch (Exception e) {e.printStackTrace(); System.exit(1);}
				double alpha = (prior == 0) ? minLog : Math.log(prior) * _beta;

				for (int d = 0; d < D; d++)
				{
					double tmp = _inPortMsgs[d][m];
					double logtmp = (tmp == 0) ? minLog : Math.log(tmp);
					_logInPortMsgs[d][m] = logtmp;
					alpha += logtmp;
				}
				alphas[m] = alpha;
		}

		//Now compute output messages for each outgoing edge
		for (int out_d = 0; out_d < D; out_d++ )
		{
			double[] outMsgs = _outMsgArray[out_d].messageValues;


			double maxLog = Double.NEGATIVE_INFINITY;

			//set outMsgs to alpha - mu_d,m
			//find max alpha
			double[] logInPortMsgsD = _logInPortMsgs[out_d];
			for (int m = 0; m < M; m++)
			{
				double out = alphas[m] - logInPortMsgsD[m];

				// Subtract the log weight
				out -= _logWeight[m];

				if (out > maxLog) maxLog = out;
				outMsgs[m] = out;
			}

			//create sum
			double sum = 0;
			for (int m = 0; m < M; m++)
			{
				double out = Math.exp(outMsgs[m] - maxLog);
				outMsgs[m] = out;
				sum += out;
			}

			//calculate message by dividing by sum
			for (int m = 0; m < M; m++)
			{
				outMsgs[m] /= sum;
			}

		}
	}

	public void resample()
	{
		int numPorts = _var.getSiblings().size();
		double _lowerBound = _domain.getLowerBound();
		double _upperBound = _domain.getUpperBound();
		int M = _numParticles;


		// For each sample value
		for (int m = 0; m < M; m++)
		{
			double sampleValue = _particleValues[m];
			double potential = 0;
			double potentialProposed = 0;


			// Start with the potential for the current particle value
			if (_input != null)
				try {
					potential = -Math.log(_input.eval(new Object[]{sampleValue})) * _beta;
				} 
				catch (Exception e) 
				{
					e.printStackTrace(); System.exit(1);
				}

			for (int portIndex = 0; portIndex < numPorts; portIndex++)
			{
				INode factorNode = _var.getSiblings().get(portIndex);
				int factorPortNumber = 0;
				try {
					factorPortNumber = factorNode.getPortNum(_var);
					} 
				catch (Exception e) 
				{
					e.printStackTrace(); System.exit(1);
				}
				
				SRealFactor factor = (SRealFactor)(_var.getSiblings().get(portIndex).getSolver());
				potential += factor.getMarginalPotential(sampleValue, factorPortNumber);
			}


			// Now repeat resampling this sample
			for (int update = 0; update < _resamplingUpdatesPerSample; update++)
			{
				double proposalDelta = _resamplingProposalStdDev * ParticleBPSolverRandomGenerator.rand.nextGaussian();
				double proposalValue = sampleValue + proposalDelta;

				// If outside the bounds, then reject
				if (proposalValue < _lowerBound) continue;
				if (proposalValue > _upperBound) continue;

				// Sum up the potentials from the input and all connected factors
				potentialProposed = 0;
				if (_input != null)
					try {potentialProposed = -Math.log(_input.eval(new Object[]{proposalValue})) * _beta;} catch (Exception e) {e.printStackTrace(); System.exit(1);}

					for (int portIndex = 0; portIndex < numPorts; portIndex++)
					{
						INode factorNode = _var.getSiblings().get(portIndex);
						int factorPortNumber = 0;
						try {factorPortNumber = factorNode.getPortNum(_var);} catch (Exception e) {e.printStackTrace(); System.exit(1);}
						SRealFactor factor = (SRealFactor)(_var.getSiblings().get(portIndex).getSolver());
						potentialProposed += factor.getMarginalPotential(proposalValue, factorPortNumber);
					}


					// Accept or reject
					double rejectionThreshold = Math.exp(potential - potentialProposed);	// Note, no Hastings factor if Gaussian proposal distribution
					if (ParticleBPSolverRandomGenerator.rand.nextDouble() < rejectionThreshold)
					{
						sampleValue = proposalValue;
						potential = potentialProposed;
					}
			}

			_particleValues[m] = sampleValue;	// Keep this sample
			_logWeight[m] = -potential;			// Sum-product code uses log(p) instead of -log(p)


			// Update the incoming messages for the new particle value
			for (int d = 0; d < numPorts; d++)
			{
				INode factorNode = _var.getSiblings().get(d);
				int factorPortNumber = 0;
				try {factorPortNumber = factorNode.getPortNum(_var);} catch (Exception e) {e.printStackTrace(); System.exit(1);}
				SRealFactor factor = (SRealFactor)(_var.getSiblings().get(d).getSolver());
				_inPortMsgs[d][m] = Math.exp(factor.getMarginalPotential(sampleValue, factorPortNumber));
			}


		}

		// Update the outgoing messages associated with the new particle locations
		update();

		// Indicate that the particles have been updated
		for (int iPort = 0; iPort < numPorts; iPort++)
			_outMsgArray[iPort].resamplingVersion++;
	}


	public Object getBelief() 
	{
		final double minLog = -100;
		int M = _numParticles;
		int D = _var.getSiblings().size();
		double maxLog = Double.NEGATIVE_INFINITY;


		double[] outBelief = new double[M];

		for (int m = 0; m < M; m++)
		{
			double prior = 1;
			if (_input != null)
				prior = _input.eval(new Object[]{_particleValues[m]});
			double out = (prior == 0) ? minLog : Math.log(prior) * _beta;

			for (int d = 0; d < D; d++)
			{
				double tmp = _inPortMsgs[d][m];
				out += (tmp == 0) ? minLog : Math.log(tmp);
			}

			//	        // Subtract the log weight
			//	        out -= _logWeight[m];

			if (out > maxLog) maxLog = out;
			outBelief[m] = out;
		}

		//create sum
		double sum = 0;
		for (int m = 0; m < M; m++)
		{
			double out = Math.exp(outBelief[m] - maxLog);
			outBelief[m] = out;
			sum += out;
		}

		//calculate belief by dividing by sum
		for (int m = 0; m < M; m++)
			outBelief[m] /= sum;

		return outBelief;
	}

	// Alternative belief, returned for a specified set of variable values
	public double [] getBelief(double[] valueSet) 
	{
		final double minLog = -100;
		int M = valueSet.length;
		int D = _var.getSiblings().size();
		double maxLog = Double.NEGATIVE_INFINITY;


		double[] outBelief = new double[M];

		for (int m = 0; m < M; m++)
		{
			double value = valueSet[m];
			double prior = 1;
			if (_input != null)
				prior = _input.eval(new Object[]{value});
			double out = (prior == 0) ? minLog : Math.log(prior) * _beta;

			for (int d = 0; d < D; d++)
			{
				INode factorNode = _var.getSiblings().get(d);
				int factorPortNumber = factorNode.getPortNum(_var);
				SRealFactor factor = (SRealFactor)(_var.getSiblings().get(d).getSolver());
				out -= factor.getMarginalPotential(value, factorPortNumber);	// Potential is -log(p)
			}

			if (out > maxLog) maxLog = out;
			outBelief[m] = out;
		}

		//create sum
		double sum = 0;
		for (int m = 0; m < M; m++)
		{
			double out = Math.exp(outBelief[m] - maxLog);
			outBelief[m] = out;
			sum += out;
		}

		//calculate belief by dividing by sum
		for (int m = 0; m < M; m++)
			outBelief[m] /= sum;

		return outBelief;
	}

	public double[] getParticleValues()
	{
		double[] particles = new double[_numParticles];
		for (int i = 0; i < _numParticles; i++) particles[i] = _particleValues[i];
		return particles;
	}

	public void setNumParticles(int numParticles)
	{
		_numParticles = numParticles;
		for (int i = 0; i < _var.getSiblings().size(); i++)
			_var.getFactors()[i].getSolver().createMessages();
	}
	public int getNumParticles() {return _numParticles;}

	public void setResamplingUpdatesPerParticle(int updatesPerParticle) {_resamplingUpdatesPerSample = updatesPerParticle;}
	public int getResamplingUpdatesPerParticle() {return _resamplingUpdatesPerSample;}

	public void setProposalStandardDeviation(double stdDev) {_resamplingProposalStdDev = stdDev;}
	public double getProposalStandardDeviation() {return _resamplingProposalStdDev;}

	// Sets the range of initial particle values
	// Overrides the domain (if one is specified) in determining the initial particle values
	public void setInitialParticleRange(double min, double max)
	{
		_initialParticleRangeSet = true;
		_initialParticleMin = min;
		_initialParticleMax = max;
	}

	public void setBeta(double beta)	// beta = 1/temperature
	{
		_beta = beta;
	}

	public void setInput(Object input) 
	{
		if (input == null)
			_input = null;
		else
			_input = (FactorFunction)input;
	}

	public double getScore()
	{
		if (_guessWasSet)
			return _input.evalEnergy(_guessValue);
		else
			throw new DimpleException("This solver doesn't provide a default value. Must set guesses for all variables.");
	}


	public void remove(Factor factor)
	{
	}
	
	@Override
	public Object[] createMessages(ISolverFactor factor) 
	{
		_particleValues = new Double[_numParticles];
		for (int i = 0; i < _particleValues.length; i++)
			_particleValues[i] = 0.0;
		_logWeight = new double[_numParticles];

		int portNum = _var.getPortNum(factor.getModelObject());
		int numPorts = Math.max(_inPortMsgs.length, portNum+1);
		
		_inPortMsgs = Arrays.copyOf(_inPortMsgs,numPorts);
		_logInPortMsgs = Arrays.copyOf(_logInPortMsgs,numPorts);
		_outMsgArray = Arrays.copyOf(_outMsgArray,numPorts); 
		

		_inPortMsgs[portNum] = getDefaultFactorToVariableMessage();
		_logInPortMsgs[portNum] = new double[_numParticles];

			// Overwrite the output message so that it can be created in the correct form
		_outMsgArray[portNum] = getDefaultVariableToFactorMessage();
	
		return new Object [] {_inPortMsgs[portNum],_outMsgArray[portNum]};
	}


	@Override
	public void resetEdgeMessages(int portNum) 
	{
		_inPortMsgs[portNum] = (double[])resetInputMessage(_inPortMsgs[portNum]);
		_outMsgArray[portNum] = (ParticleBPSolverVariableToFactorMessage)resetOutputMessage(_outMsgArray[portNum]);
	}

	
	public ParticleBPSolverVariableToFactorMessage getDefaultVariableToFactorMessage()
	{
		ParticleBPSolverVariableToFactorMessage message = new ParticleBPSolverVariableToFactorMessage(_numParticles);
		return (ParticleBPSolverVariableToFactorMessage)resetOutputMessage(message);
	}

	public double [] getDefaultFactorToVariableMessage()
	{
		double[] retVal = new double[_numParticles];
		retVal = (double[])resetInputMessage(retVal);
		return retVal;
	}

	@Override
	public Object resetInputMessage(Object message)
	{
		double[] tmp = (double[])message;
		double val = 1.0/tmp.length;
		for (int i = 0; i < tmp.length; i++)
			tmp[i] = val;

		return tmp;
	}

	@Override
	public Object resetOutputMessage(Object message) 
	{
		
		double particleMin = 0;
		double particleMax = 0;
		
		if (_initialParticleRangeSet)
		{
			particleMin = _initialParticleMin;
			particleMax = _initialParticleMax;
		}
		else if (_var.getDomain() != null)
		{
			if (!Double.isInfinite(_domain.getLowerBound()) && !Double.isInfinite(_domain.getUpperBound()))
			{
				particleMin = _domain.getLowerBound();
				particleMax = _domain.getUpperBound();
			}
		}

		int length = _particleValues.length;
    	double particleIncrement = (length > 1) ? (particleMax - particleMin) / (length - 1) : 0;
    	double particleValue = particleMin;
    	
    	for (int i = 0; i < length; i++)
    	{
    		_particleValues[i] = particleValue;
    		particleValue += particleIncrement;
    	}

		ParticleBPSolverVariableToFactorMessage m = (ParticleBPSolverVariableToFactorMessage)message;
		
		m.particleValues = _particleValues;
		
		return m;
	}

	
	@Override
	public Object getInputMsg(int portIndex) 
	{
		return _inPortMsgs[portIndex];
	}

	@Override
	public Object getOutputMsg(int portIndex) 
	{
		return _outMsgArray[portIndex];
	}

	@Override
	public void setInputMsg(int portIndex, Object obj) 
	{
		_inPortMsgs[portIndex] = (double[])obj;
		
	}

	@Override
	public void moveMessages(ISolverNode other, int thisPortNum,
			int otherPortNum) 
	{
		throw new DimpleException("not supported");
		
	}

}
