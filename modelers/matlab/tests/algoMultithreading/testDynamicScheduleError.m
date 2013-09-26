%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%   Copyright 2013 Analog Devices, Inc.
%
%   Licensed under the Apache License, Version 2.0 (the "License");
%   you may not use this file except in compliance with the License.
%   You may obtain a copy of the License at
%
%       http://www.apache.org/licenses/LICENSE-2.0
%
%   Unless required by applicable law or agreed to in writing, software
%   distributed under the License is distributed on an "AS IS" BASIS,
%   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
%   See the License for the specific language governing permissions and
%   limitations under the License.
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

function testDynamicScheduleError()

    for mode = 0:2
        N = 2;
        b = Bit(N);
        fg = FactorGraph();
        fg.Scheduler='RandomWithReplacementScheduler';
        fg.addFactorVectorized(@(a,b) rand(), b(:,1:end-1),b(:,2:end));
        fg.addFactorVectorized(@(a,b) rand(), b(1:end-1,:),b(2:end,:));
        b.Input = rand(N,N);
        fg.NumIterations = 5;    
        numThreads = 16;
        fg.Solver.setMultiThreadMode(mode);
        fg.Solver.setNumThreads(numThreads);

        m = '';
        try
            fg.solve();
        catch e
            m = e.message;
        end
        assertTrue(~isempty(findstr(m,'Cannot currently create dependency graph of Dynamic Schedule')));
    end
end